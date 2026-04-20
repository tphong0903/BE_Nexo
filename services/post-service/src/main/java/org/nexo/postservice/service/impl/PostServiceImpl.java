package org.nexo.postservice.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.dto.MessageDTO;
import org.nexo.postservice.dto.MessagePostDTO;
import org.nexo.postservice.dto.PostRequestDTO;
import org.nexo.postservice.dto.UserTagDTO;
import org.nexo.postservice.dto.*;
import org.nexo.postservice.dto.UserActivityEvent;
import org.nexo.postservice.dto.response.PageModelResponse;
import org.nexo.postservice.dto.response.PostResponseDTO;
import org.nexo.postservice.dto.response.ReelResponseDTO;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.PostMediaModel;
import org.nexo.postservice.model.PostModel;
import org.nexo.postservice.model.ReelModel;
import org.nexo.postservice.repository.IPostMediaRepository;
import org.nexo.postservice.repository.IPostRepository;
import org.nexo.postservice.repository.IReelRepository;
import org.nexo.postservice.service.GrpcServiceImpl.client.InteractionGrpcClient;
import org.nexo.postservice.service.GrpcServiceImpl.client.UserGrpcClient;
import org.nexo.postservice.service.IHashTagService;
import org.nexo.postservice.service.IPostService;
import org.nexo.postservice.util.Enum.ENotificationType;
import org.nexo.postservice.util.Enum.EVisibilityPost;
import org.nexo.postservice.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements IPostService {
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    private final FileService fileServiceClient;
    private final SecurityUtil securityUtil;
    private final IPostRepository postRepository;
    private final IReelRepository reelRepository;
    private final UserGrpcClient userGrpcClient;
    private final InteractionGrpcClient interactionGrpcClient;
    private final IPostMediaRepository postMediaRepository;
    private final IHashTagService hashTagService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public String savePost(PostRequestDTO request, List<MultipartFile> files) {
        securityUtil.checkOwner(request.getUserId());
        UserServiceProto.UserDTOResponse userDTO = userGrpcClient.getUserDTOById(request.getUserId());

        PostModel model;
        String oldTag = "";
        boolean isNew = (request.getPostId() == 0);

        if (!isNew) {
            model = postRepository.findById(request.getPostId())
                    .orElseThrow(() -> new CustomException("Post not found", HttpStatus.BAD_REQUEST));
            oldTag = model.getTag();

            List<PostMediaModel> oldMedias = postMediaRepository.findAllByPostModel_Id(model.getId());
            List<PostMediaModel> toDelete = oldMedias.stream()
                    .filter(m -> !request.getMediaUrl().contains(m.getMediaUrl()))
                    .toList();
            if (!toDelete.isEmpty())
                postMediaRepository.deleteAllInBatch(toDelete);
        } else {
            model = PostModel.builder()
                    .userId(request.getUserId())
                    .commentQuantity(0L)
                    .likeQuantity(0L)
                    .build();
        }

        model.setCaption(request.getCaption());
        model.setTag(request.getTag());
        model.setVisibility(EVisibilityPost.valueOf(request.getVisibility()));
        model.setIsActive(true);
        model.setAuthorName(userDTO.getUsername());

        postRepository.save(model);
        if (files != null && !files.isEmpty() && !files.getFirst().isEmpty()) {
            String token = ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken()
                    .getTokenValue();
            fileServiceClient.savePostMedia(files, model.getId(), token);
        }

        if (isNew) {
            kafkaTemplate.send("post-created", MessagePostDTO.builder()
                    .postId(model.getId())
                    .authorId(request.getUserId())
                    .createdAt(Instant.now().toEpochMilli())
                    .build());

            UserActivityEvent activityEvent = UserActivityEvent.builder()
                    .eventType("POST_CREATED")
                    .userId(request.getUserId())
                    .targetId(model.getId())
                    .targetType("POST")
                    .occurredAt(Instant.now())
                    .metadata("{\"source\":\"post-service\"}")
                    .build();
            kafkaTemplate.send("user-events", String.valueOf(request.getUserId()), activityEvent);
        }

        hashTagService.findAndAddHashTagFromCaption(model);
        tagUserIntoPost(oldTag, request.getTag(), request.getUserId(), model.getId());

        clearPostCache(model.getId());

        return "Success";
    }

    @Override
    @Transactional
    public String saveReel(PostRequestDTO request, List<MultipartFile> files) {
        securityUtil.checkOwner(request.getUserId());
        UserServiceProto.UserDTOResponse userDTO = userGrpcClient.getUserDTOById(request.getUserId());

        ReelModel model;
        boolean isNew = (request.getPostId() == 0);

        if (!isNew) {
            model = reelRepository.findById(request.getPostId())
                    .orElseThrow(() -> new CustomException("Reel not found", HttpStatus.BAD_REQUEST));
        } else {
            model = ReelModel.builder()
                    .userId(request.getUserId())
                    .commentQuantity(0L)
                    .likeQuantity(0L)
                    .build();
        }

        model.setCaption(request.getCaption());
        model.setVisibility(EVisibilityPost.valueOf(request.getVisibility()));
        model.setIsActive(true);
        model.setAuthorName(userDTO.getUsername());

        reelRepository.save(model);

        if (files != null && !files.isEmpty() && !files.getFirst().isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String token = ((JwtAuthenticationToken) auth).getToken().getTokenValue();
            fileServiceClient.saveReelMedia(files, model.getId(), token);
        }

        if (isNew) {
            kafkaTemplate.send("reel-created", MessagePostDTO.builder()
                    .postId(model.getId())
                    .authorId(request.getUserId())
                    .createdAt(Instant.now().toEpochMilli())
                    .build());
        }

        hashTagService.findAndAddHashTagFromCaption(model);

        clearReelCache(model.getId()); // Cập nhật Cache

        return "Success";
    }

    @Override
    @Transactional
    public String inactivePost(Long id) {
        PostModel model = postRepository.findById(id)
                .orElseThrow(() -> new CustomException("Post not exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());

        model.setIsActive(!model.getIsActive());
        postRepository.save(model);
        clearPostCache(id);

        return "Success";
    }

    @Override
    @Transactional
    public String inactiveReel(Long id) {
        ReelModel model = reelRepository.findById(id)
                .orElseThrow(() -> new CustomException("Reel not exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());

        model.setIsActive(!model.getIsActive());
        reelRepository.save(model);
        clearReelCache(id);

        return "Success";
    }

    @Override
    @Transactional
    public String deletePost(Long id) {
        PostModel model = postRepository.findById(id)
                .orElseThrow(() -> new CustomException("Post not found", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());

        postRepository.delete(model);
        clearPostCache(id);
        redisTemplate.opsForZSet().remove("feed:" + model.getUserId(), id);

        return "Success";
    }

    @Override
    @Transactional
    public String deleteReel(Long id) {
        ReelModel model = reelRepository.findById(id)
                .orElseThrow(() -> new CustomException("Reel not found", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());

        reelRepository.delete(model);
        clearReelCache(id);

        redisTemplate.opsForZSet().remove("feed:" + model.getUserId(), id);

        return "Success";
    }

    @Override
    public PostResponseDTO getPostById(Long id) {
        Long viewerId = securityUtil.getUserIdFromToken();

        PostResponseDTO cachedPost = getPostCacheDTO(id);

        checkVisibilityAccess(cachedPost.getUserId(), viewerId, EVisibilityPost.valueOf(cachedPost.getVisibility()));

        return buildDynamicPostResponse(cachedPost, viewerId);
    }

    @Override
    public ReelResponseDTO getReelById(Long id) {
        Long viewerId = securityUtil.getUserIdFromToken();

        ReelResponseDTO cachedReel = getReelCacheDTO(id);
        checkVisibilityAccess(cachedReel.getUserId(), viewerId, EVisibilityPost.valueOf(cachedReel.getVisibility()));

        return buildDynamicReelResponse(cachedReel, viewerId);
    }

    @Override
    public PostResponseDTO getPostByIdGrpc(Long id) {
        PostResponseDTO cachedPost = getPostCacheDTO(id);
        return updateDynamicCountersForPost(cachedPost);
    }

    @Override
    public ReelResponseDTO getReelByIdGrpc(Long id) {
        ReelResponseDTO cachedReel = getReelCacheDTO(id);
        return updateDynamicCountersForReel(cachedReel);
    }

    private PostResponseDTO getPostCacheDTO(Long postId) {
        String cacheKey = "post_dto_cache:" + postId;
        PostResponseDTO cachedDto = (PostResponseDTO) redisTemplate.opsForValue().get(cacheKey);

        if (cachedDto != null) {
            return cachedDto;
        }

        PostModel dbPost = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException("Post not found", HttpStatus.BAD_REQUEST));

        UserServiceProto.UserDTOResponse authorInfo = userGrpcClient.getUserDTOById(dbPost.getUserId());

        PostResponseDTO staticDto = convertToPostResponseDTO(dbPost, authorInfo, false);

        redisTemplate.opsForValue().set(cacheKey, staticDto, CACHE_TTL);
        return staticDto;
    }

    private ReelResponseDTO getReelCacheDTO(Long reelId) {
        String cacheKey = "reel_dto_cache:" + reelId;
        ReelResponseDTO cachedDto = (ReelResponseDTO) redisTemplate.opsForValue().get(cacheKey);

        if (cachedDto != null) {
            return cachedDto;
        }

        ReelModel dbReel = reelRepository.findById(reelId)
                .orElseThrow(() -> new CustomException("Reel not found", HttpStatus.BAD_REQUEST));

        UserServiceProto.UserDTOResponse authorInfo = userGrpcClient.getUserDTOById(dbReel.getUserId());

        ReelResponseDTO staticDto = convertToReelResponseDTO(dbReel, authorInfo, false);

        redisTemplate.opsForValue().set(cacheKey, staticDto, CACHE_TTL);
        return staticDto;
    }

    private PostResponseDTO buildDynamicPostResponse(PostResponseDTO staticDto, Long viewerId) {
        Map<Long, Boolean> likedMap = interactionGrpcClient.checkBatchLikesPost(viewerId,
                List.of(staticDto.getPostId()));
        boolean isLiked = likedMap.getOrDefault(staticDto.getPostId(), false);
        PostResponseDTO updatedDto = updateDynamicCountersForPost(staticDto);
        return updatedDto.toBuilder()
                .isLike(isLiked)
                .build();
    }

    private PostResponseDTO updateDynamicCountersForPost(PostResponseDTO dto) {
        Object likesStr = redisTemplate.opsForValue().get("post:likes:" + dto.getPostId());
        Object commentsStr = redisTemplate.opsForValue().get("post:comments:" + dto.getPostId());

        Long likes = (likesStr != null) ? Long.valueOf(likesStr.toString()) : dto.getQuantityLike();
        Long comments = (commentsStr != null) ? Long.valueOf(commentsStr.toString()) : dto.getQuantityComment();

        return dto.toBuilder()
                .quantityLike(likes)
                .quantityComment(comments)
                .build();
    }

    private ReelResponseDTO buildDynamicReelResponse(ReelResponseDTO staticDto, Long viewerId) {
        Map<Long, Boolean> likedMap = interactionGrpcClient.checkBatchLikesReel(viewerId,
                List.of(staticDto.getReelId()));
        boolean isLiked = likedMap.getOrDefault(staticDto.getReelId(), false);

        ReelResponseDTO updatedDto = updateDynamicCountersForReel(staticDto);

        return updatedDto.toBuilder()
                .isLike(isLiked)
                .build();
    }

    private ReelResponseDTO updateDynamicCountersForReel(ReelResponseDTO dto) {
        Object likesStr = redisTemplate.opsForValue().get("reel:likes:" + dto.getReelId());
        Object commentsStr = redisTemplate.opsForValue().get("reel:comments:" + dto.getReelId());

        Long likes = (likesStr != null) ? Long.valueOf(likesStr.toString()) : dto.getQuantityLike();
        Long comments = (commentsStr != null) ? Long.valueOf(commentsStr.toString()) : dto.getQuantityComment();

        return dto.toBuilder()
                .quantityLike(likes)
                .quantityComment(comments)
                .build();
    }

    private void clearPostCache(Long id) {
        redisTemplate.delete(Arrays.asList("post_dto_cache:" + id, "post:likes:" + id, "post:comments:" + id));
    }

    private void clearReelCache(Long id) {
        redisTemplate.delete(Arrays.asList("reel_dto_cache:" + id, "reel:likes:" + id, "reel:comments:" + id));
    }

    @Override
    public PageModelResponse getAllPostOfUser(Long targetUserId, int page, int limit) {
        Long viewerId = securityUtil.getUserIdFromToken();
        EVisibilityPost visibilityLimit = checkVisibilityAccess(targetUserId, viewerId, null);

        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        Page<PostModel> postPage = (visibilityLimit == null)
                ? postRepository.findByUserIdAndIsActive(targetUserId, true, pageable)
                : postRepository.findByUserIdAndIsActiveAndVisibility(targetUserId, true, visibilityLimit, pageable);

        UserServiceProto.UserDTOResponse authorInfo = userGrpcClient.getUserDTOById(targetUserId);
        List<Long> postIds = postPage.getContent().stream().map(PostModel::getId).toList();
        Map<Long, Boolean> likedMap = interactionGrpcClient.checkBatchLikesPost(viewerId, postIds);

        List<PostResponseDTO> content = postPage.getContent().stream()
                .map(post -> convertToPostResponseDTO(post, authorInfo, likedMap.getOrDefault(post.getId(), false)))
                .toList();

        return buildPageResponse(postPage, content);
    }

    @Override
    public PageModelResponse getAllReelOfUser(Long targetUserId, int page, int limit) {
        Long viewerId = securityUtil.getUserIdFromToken();
        EVisibilityPost visibilityLimit = checkVisibilityAccess(targetUserId, viewerId, null);

        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        Page<ReelModel> reelPage = (visibilityLimit == null)
                ? reelRepository.findByUserIdAndIsActive(targetUserId, true, pageable)
                : reelRepository.findByUserIdAndIsActiveAndVisibility(targetUserId, true, visibilityLimit, pageable);

        UserServiceProto.UserDTOResponse authorInfo = userGrpcClient.getUserDTOById(targetUserId);
        List<Long> reelIds = reelPage.getContent().stream().map(ReelModel::getId).toList();
        Map<Long, Boolean> likedMap = interactionGrpcClient.checkBatchLikesReel(viewerId, reelIds);

        List<ReelResponseDTO> content = reelPage.getContent().stream()
                .map(reel -> convertToReelResponseDTO(reel, authorInfo, likedMap.getOrDefault(reel.getId(), false)))
                .toList();

        return buildPageResponse(reelPage, content);
    }

    @Override
    public List<PostResponseDTO> getPostsByIds(List<Long> postIds, Long viewerId) {
        if (postIds == null || postIds.isEmpty())
            return Collections.emptyList();

        List<PostModel> posts = postRepository.findAllById(postIds);

        List<Long> authorIds = posts.stream().map(PostModel::getUserId).distinct().toList();
        Map<Long, UserServiceProto.UserDTOResponse2> authorMap = userGrpcClient.getUsersByIds(authorIds).stream()
                .collect(Collectors.toMap(UserServiceProto.UserDTOResponse2::getId, u -> u));

        Map<Long, Boolean> likedMap = interactionGrpcClient.checkBatchLikesPost(viewerId, postIds);

        return posts.stream().map(post -> {
            UserServiceProto.UserDTOResponse2 author2 = authorMap.get(post.getUserId());
            UserServiceProto.UserDTOResponse authorDto = UserServiceProto.UserDTOResponse.newBuilder()
                    .setId(author2.getId()).setUsername(author2.getUsername()).setAvatar(author2.getAvatar()).build();

            return convertToPostResponseDTO(post, authorDto, likedMap.getOrDefault(post.getId(), false));
        }).toList();
    }

    @Override
    public List<ReelResponseDTO> getReelsByIds(List<Long> reelIds, Long viewerId) {
        if (reelIds == null || reelIds.isEmpty())
            return Collections.emptyList();

        List<ReelModel> reels = reelRepository.findAllById(reelIds);

        List<Long> authorIds = reels.stream().map(ReelModel::getUserId).distinct().toList();
        Map<Long, UserServiceProto.UserDTOResponse2> authorMap = userGrpcClient.getUsersByIds(authorIds).stream()
                .collect(Collectors.toMap(UserServiceProto.UserDTOResponse2::getId, u -> u));

        Map<Long, Boolean> likedMap = interactionGrpcClient.checkBatchLikesReel(viewerId, reelIds);

        return reels.stream().map(reel -> {
            UserServiceProto.UserDTOResponse2 author2 = authorMap.get(reel.getUserId());
            UserServiceProto.UserDTOResponse authorDto = UserServiceProto.UserDTOResponse.newBuilder()
                    .setId(author2.getId()).setUsername(author2.getUsername()).setAvatar(author2.getAvatar()).build();

            return convertToReelResponseDTO(reel, authorDto, likedMap.getOrDefault(reel.getId(), false));
        }).toList();
    }

    @Override
    public PageModelResponse<PostResponseDTO> getPopularPosts(int page, int size, String hashtag) {
        Pageable pageable = PageRequest.of(page, size);
        Long id = securityUtil.getUserIdFromToken();
        UserServiceProto.UserDTOResponse currentUser = userGrpcClient.getUserDTOById(id);

        Page<PostModel> postPage = hashtag.isEmpty()
                ? postRepository.findPopularPublicPostsWithHashtagScore(pageable)
                : postRepository.findPopularPublicPostsByHashtag(hashtag, pageable);

        List<Long> postIds = postPage.getContent().stream().map(PostModel::getId).toList();
        Map<Long, Boolean> likedMap = interactionGrpcClient.checkBatchLikesPost(currentUser.getId(), postIds);

        List<PostResponseDTO> postDTOs = postPage.getContent().stream()
                .map(post -> convertToPostResponseDTO(post, currentUser, likedMap.getOrDefault(post.getId(), false)))
                .toList();

        return buildPageResponse(postPage, postDTOs);
    }

    private PostResponseDTO convertToPostResponseDTO(PostModel model, UserServiceProto.UserDTOResponse author,
            Boolean isLike) {
        List<Long> tagIds = parseTagString(model.getTag(), model.getUserId());
        List<UserTagDTO> userTags = tagIds.isEmpty() ? Collections.emptyList()
                : userGrpcClient.getUsersByIds(tagIds).stream()
                        .map(u -> UserTagDTO.builder().userId(u.getId()).userName(u.getUsername()).build())
                        .toList();

        Object likesStr = redisTemplate.opsForValue().get("post:likes:" + model.getId());
        Object commentsStr = redisTemplate.opsForValue().get("post:comments:" + model.getId());
        Long likes = (likesStr != null) ? Long.valueOf(likesStr.toString()) : model.getLikeQuantity();
        Long comments = (commentsStr != null) ? Long.valueOf(commentsStr.toString()) : model.getCommentQuantity();

        return PostResponseDTO.builder()
                .postId(model.getId())
                .userName(author.getUsername())
                .avatarUrl(author.getAvatar())
                .visibility(model.getVisibility().toString())
                .tag(model.getTag())
                .listUserTag(userTags)
                .caption(model.getCaption())
                .createdAt(model.getCreatedAt())
                .isActive(model.getIsActive())
                .isLike(isLike)
                .quantityLike(likes)
                .quantityComment(comments)
                .userId(model.getUserId())
                .mediaUrl(model.getPostMediaModels() != null
                        ? model.getPostMediaModels().stream().map(PostMediaModel::getMediaUrl).toList()
                        : List.of())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    private ReelResponseDTO convertToReelResponseDTO(ReelModel model, UserServiceProto.UserDTOResponse author,
            Boolean isLike) {
        Object likesStr = redisTemplate.opsForValue().get("reel:likes:" + model.getId());
        Object commentsStr = redisTemplate.opsForValue().get("reel:comments:" + model.getId());
        Long likes = (likesStr != null) ? Long.valueOf(likesStr.toString()) : model.getLikeQuantity();
        Long comments = (commentsStr != null) ? Long.valueOf(commentsStr.toString()) : model.getCommentQuantity();

        return ReelResponseDTO.builder()
                .reelId(model.getId())
                .userName(author.getUsername())
                .avatarUrl(author.getAvatar())
                .visibility(model.getVisibility().toString())
                .caption(model.getCaption())
                .createdAt(model.getCreatedAt())
                .isActive(model.getIsActive())
                .quantityLike(likes)
                .isLike(isLike)
                .quantityComment(comments)
                .userId(model.getUserId())
                .mediaUrl(model.getVideoUrl())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    private EVisibilityPost checkVisibilityAccess(Long targetUserId, Long viewerId,
            EVisibilityPost requiredVisibility) {
        if (targetUserId.equals(viewerId))
            return null;

        if (securityUtil.isPrivilegedUser()) {
            return EVisibilityPost.PUBLIC;
        }

        if (securityUtil.isPrivilegedUser()) {
            return EVisibilityPost.PUBLIC;
        }

        UserServiceProto.CheckFollowResponse followCheck = userGrpcClient.checkFollow(viewerId, targetUserId);
        if (followCheck.getIsPrivate() && !followCheck.getIsFollow()) {
            throw new CustomException("Don't have permission to view this content", HttpStatus.FORBIDDEN);
        }

        if (requiredVisibility != null && requiredVisibility == EVisibilityPost.PRIVATE) {
            throw new CustomException("This content is private", HttpStatus.FORBIDDEN);
        }

        return EVisibilityPost.PUBLIC;
    }

    private List<Long> parseTagString(String tagStr, Long excludeId) {
        if (tagStr == null || tagStr.isBlank())
            return Collections.emptyList();
        return Arrays.stream(tagStr.split(","))
                .filter(s -> !s.isBlank())
                .map(Long::parseLong)
                .filter(id -> !id.equals(excludeId))
                .toList();
    }

    private <T> PageModelResponse<T> buildPageResponse(Page<?> pageData, List<T> content) {
        return PageModelResponse.<T>builder()
                .pageNo(pageData.getNumber())
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .last(pageData.isLast())
                .content(content)
                .build();
    }

    public void tagUserIntoPost(String oldTag, String users, Long currentUserId, Long postId) {
        List<Long> oldTagIds = parseTagString(oldTag, currentUserId);
        List<Long> tagIds = parseTagString(users, currentUserId);

        for (Long id : tagIds) {
            if (!oldTagIds.contains(id)) {
                MessageDTO messageDTO = MessageDTO.builder()
                        .actorId(currentUserId)
                        .recipientId(id)
                        .notificationType(String.valueOf(ENotificationType.TAG))
                        .targetUrl("/posts/" + postId)
                        .build();
                kafkaTemplate.send("notification", messageDTO);
            }
        }
    }
}
