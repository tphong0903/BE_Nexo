package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.dto.*;
import org.nexo.postservice.dto.response.PageModelResponse;
import org.nexo.postservice.dto.response.PostResponseDTO;
import org.nexo.postservice.dto.response.ReelResponseDTO;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.PostMediaModel;
import org.nexo.postservice.model.PostModel;
import org.nexo.postservice.model.ReelModel;
import org.nexo.postservice.repository.AdminContentRepository;
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

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements IPostService {
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
    private final AdminContentRepository adminContentRepository;

    @Override
    public String savePost(PostRequestDTO postRequestDTO, List<MultipartFile> files) {
        securityUtil.checkOwner(postRequestDTO.getUserId());
        UserServiceProto.UserDTOResponse userDTOResponse = userGrpcClient.getUserDTOById(postRequestDTO.getUserId());
        PostModel model;

        String oldTag = "";
        if (postRequestDTO.getPostId() != 0) {
            List<PostMediaModel> postMediaModelList = postMediaRepository.findAllByPostModel_Id(postRequestDTO.getPostId());
            for (PostMediaModel postMediaModel : postMediaModelList) {
                if (!postRequestDTO.getMediaUrl().contains(postMediaModel.getMediaUrl()))
                    postMediaRepository.delete(postMediaModel);
            }
            model = postRepository.findById(postRequestDTO.getPostId())
                    .orElseThrow(() -> new CustomException("Post not found", HttpStatus.BAD_REQUEST));
            oldTag = model.getTag();
            model.setCaption(postRequestDTO.getCaption());
            model.setTag(postRequestDTO.getTag());
            model.setVisibility(EVisibilityPost.valueOf(postRequestDTO.getVisibility()));
            model.setIsActive(true);
        } else {
            model = PostModel.builder()
                    .userId(postRequestDTO.getUserId())
                    .caption(postRequestDTO.getCaption())
                    .tag(postRequestDTO.getTag())
                    .commentQuantity(0L)
                    .likeQuantity(0L)
                    .visibility(EVisibilityPost.valueOf(postRequestDTO.getVisibility()))
                    .isActive(true)
                    .build();

        }
        model.setAuthorName(userDTOResponse.getUsername());
        postRepository.save(model);
//        if (postRequestDTO.getPostId() != 0) {
//            String postKey = "post:" + model.getId();
//            String feedKey = "feed:" + model.getUserId();
//
//            redisTemplate.delete(postKey);
//            redisTemplate.opsForZSet().remove(feedKey, model.getId());
//        }

        if (files != null && !files.isEmpty() && !files.getFirst().isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String token = ((JwtAuthenticationToken) auth).getToken().getTokenValue();
            fileServiceClient.savePostMedia(files, model.getId(), token);
        }

        if (postRequestDTO.getPostId() == 0) {
            MessagePostDTO message = MessagePostDTO.builder()
                    .postId(model.getId())
                    .authorId(postRequestDTO.getUserId())
                    .createdAt(Instant.now().toEpochMilli())
                    .build();
            kafkaTemplate.send("post-created", message);
        }

        hashTagService.findAndAddHashTagFromCaption(model);
        tagUserIntoPost(oldTag, postRequestDTO.getTag(), postRequestDTO.getUserId(), model.getId());
        return "Success";
    }

    @Override
    public String saveReel(PostRequestDTO postRequestDTO, List<MultipartFile> files) {
        securityUtil.checkOwner(postRequestDTO.getUserId());
        UserServiceProto.UserDTOResponse userDTOResponse = userGrpcClient.getUserDTOById(postRequestDTO.getUserId());

        ReelModel model;
        if (postRequestDTO.getPostId() != 0) {
            model = reelRepository.findById(postRequestDTO.getPostId())
                    .orElseThrow(() -> new CustomException("Post not found", HttpStatus.BAD_REQUEST));
            model.setCaption(postRequestDTO.getCaption());
            model.setVisibility(EVisibilityPost.valueOf(postRequestDTO.getVisibility()));
            model.setIsActive(true);
        } else {
            model = ReelModel.builder()
                    .userId(postRequestDTO.getUserId())
                    .caption(postRequestDTO.getCaption())
                    .commentQuantity(0L)
                    .likeQuantity(0L)
                    .visibility(EVisibilityPost.valueOf(postRequestDTO.getVisibility()))
                    .isActive(true)
                    .build();
        }
        model.setAuthorName(userDTOResponse.getUsername());
        reelRepository.save(model);

        if (files != null && !files.isEmpty() && !files.getFirst().isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String token = ((JwtAuthenticationToken) auth).getToken().getTokenValue();
            fileServiceClient.saveReelMedia(files, model.getId(), token);
        }

        if (postRequestDTO.getPostId() == 0) {
            MessagePostDTO message = MessagePostDTO.builder()
                    .postId(model.getId())
                    .authorId(postRequestDTO.getUserId())
                    .createdAt(Instant.now().toEpochMilli())
                    .build();
            kafkaTemplate.send("reel-created", message);
        }

        hashTagService.findAndAddHashTagFromCaption(model);
        return "Success";
    }

    @Override
    public String inactivePost(Long id) {

        PostModel model = postRepository.findById(id).orElseThrow(() -> new CustomException("Post is not  exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());

        model.setIsActive(!model.getIsActive());
        postRepository.save(model);
        return "Success";
    }

    @Override
    public String inactiveReel(Long id) {
        ReelModel model = reelRepository.findById(id).orElseThrow(() -> new CustomException("Reel is not  exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());
        model.setIsActive(!model.getIsActive());
        reelRepository.save(model);
        return "Success";
    }

    @Override
    public String deletePost(Long id) {
        PostModel model = postRepository.findById(id).orElseThrow(() -> new CustomException("Post is not  exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());
        postRepository.delete(model);
        String postKey = "post:" + id;
        String likesKey = "post:likes:" + id;
        String commentsKey = "post:comments:" + id;
        String feedKey = "feed:" + model.getUserId();

        redisTemplate.delete(postKey);
        redisTemplate.delete(likesKey);
        redisTemplate.delete(commentsKey);
        redisTemplate.opsForZSet().remove(feedKey, id);
        return "Success";
    }

    @Override
    public PageModelResponse getAllPostOfUser(Long id, int page, int limit) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto currentUser = userGrpcClient.getUserByKeycloakId(keyloakId);
        Page<PostModel> listPost = Page.empty();

        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        boolean isAllow = false;
        if (id.equals(currentUser.getUserId())) {
            isAllow = true;
            listPost = postRepository.findByUserIdAndIsActive(id, true, pageable);
        } else {
            UserServiceProto.CheckFollowResponse followResponse =
                    userGrpcClient.checkFollow(currentUser.getUserId(), id);

            if (!followResponse.getIsPrivate() || followResponse.getIsFollow()) {
                listPost = postRepository.findByUserIdAndIsActiveAndVisibility(id, true, EVisibilityPost.PUBLIC, pageable);
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to get Post", HttpStatus.BAD_REQUEST);

        UserServiceProto.UserDTOResponse userInfo = userGrpcClient.getUserDTOById(id);


        List<PostModel> posts = listPost.getContent();
        List<Long> postIds = posts.stream().map(PostModel::getId).toList();
        Map<Long, Boolean> likedPostIds = interactionGrpcClient.checkBatchLikesPost(currentUser.getUserId(), postIds);
        List<PostResponseDTO> postResponseList = listPost.getContent().stream()
                .map(post -> {
                            Boolean isLike = likedPostIds.getOrDefault(post.getId(), false);
                            return convertToPostResponseDTO(post, userInfo, isLike);
                        }
                )
                .toList();
        return PageModelResponse.<PostResponseDTO>builder()
                .pageNo(listPost.getNumber())
                .pageSize(listPost.getSize())
                .totalElements(listPost.getTotalElements())
                .totalPages(listPost.getTotalPages())
                .last(listPost.isLast())
                .content(postResponseList)
                .build();
    }

    @Override
    public PageModelResponse getAllReelOfUser(Long id, int page, int limit) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto currentUser = userGrpcClient.getUserByKeycloakId(keyloakId);
        Page<ReelModel> listReel = Page.empty();

        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, limit, sort);
        boolean isAllow = false;
        if (id.equals(currentUser.getUserId())) {
            isAllow = true;
            listReel = reelRepository.findByUserId(id, pageable);
        } else {
            UserServiceProto.CheckFollowResponse followResponse =
                    userGrpcClient.checkFollow(currentUser.getUserId(), id);

            if (!followResponse.getIsPrivate() || followResponse.getIsFollow()) {
                listReel = reelRepository.findByUserIdAndIsActive(id, true, pageable);
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to get Post", HttpStatus.BAD_REQUEST);

        UserServiceProto.UserDTOResponse userInfo = userGrpcClient.getUserDTOById(id);

        List<ReelModel> reels = listReel.getContent();
        List<Long> reelIds = reels.stream().map(ReelModel::getId).toList();
        Map<Long, Boolean> likedReelIds = interactionGrpcClient.checkBatchLikesPost(currentUser.getUserId(), reelIds);
        List<ReelResponseDTO> reelResponseDTOS = reels.stream()
                .map(reel -> {
                    Boolean isLike = likedReelIds.getOrDefault(reel.getId(), false);
                    return convertToReelResponseDTO(reel, userInfo, isLike);
                })
                .toList();
        return PageModelResponse.<ReelResponseDTO>builder()
                .pageNo(listReel.getNumber())
                .pageSize(listReel.getSize())
                .totalElements(listReel.getTotalElements())
                .totalPages(listReel.getTotalPages())
                .last(listReel.isLast())
                .content(reelResponseDTOS)
                .build();
    }


    @Override
    public PostResponseDTO getPostById(Long id) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto currentUser = userGrpcClient.getUserByKeycloakId(keyloakId);
        boolean isAllow = false;
        if (id.equals(currentUser.getUserId())) {
            isAllow = true;
        } else {
            UserServiceProto.CheckFollowResponse followResponse =
                    userGrpcClient.checkFollow(currentUser.getUserId(), id);
            if (!followResponse.getIsPrivate() || followResponse.getIsFollow()) {
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to get Post", HttpStatus.BAD_REQUEST);

        PostModel model = postRepository.findById(id).orElseThrow(() -> new CustomException("Post is not exist", HttpStatus.BAD_REQUEST));

        UserServiceProto.UserDTOResponse response = userGrpcClient.getUserDTOById(model.getUserId());

        Map<Long, Boolean> likedPostIds = interactionGrpcClient.checkBatchLikesPost(currentUser.getUserId(), List.of(model.getId()));
        return convertToPostResponseDTO(model, response, likedPostIds.getOrDefault(model.getId(), false));
    }

    @Override
    public PostResponseDTO getPostById3(Long id, Boolean isLike) {
        PostModel model = postRepository.findById(id).orElse(null);
        if (model == null)
            return null;
        UserServiceProto.UserDTOResponse response = userGrpcClient.getUserDTOById(model.getUserId());
        return convertToPostResponseDTO(model, response, isLike);
    }

    @Override
    public PostResponseDTO getPostById2(Long id) {
        PostModel model = postRepository.findById(id).orElse(null);
        if (model == null)
            return null;
        UserServiceProto.UserDTOResponse response = userGrpcClient.getUserDTOById(model.getUserId());
        return convertToPostResponseDTO(model, response, false);
    }

    @Override
    public ReelResponseDTO getReelById(Long id) {
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto currentUser = userGrpcClient.getUserByKeycloakId(keyloakId);
        boolean isAllow = false;
        if (id.equals(currentUser.getUserId())) {
            isAllow = true;
        } else {
            UserServiceProto.CheckFollowResponse followResponse =
                    userGrpcClient.checkFollow(currentUser.getUserId(), id);
            if (!followResponse.getIsPrivate() || followResponse.getIsFollow()) {
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to get Reel", HttpStatus.BAD_REQUEST);

        ReelModel model = reelRepository.findById(id).orElseThrow(() -> new CustomException("Reel is not exist", HttpStatus.BAD_REQUEST));
        UserServiceProto.UserDTOResponse response = userGrpcClient.getUserDTOById(model.getUserId());
        Map<Long, Boolean> likedReelIds = interactionGrpcClient.checkBatchLikesPost(currentUser.getUserId(), List.of(model.getId()));
        return convertToReelResponseDTO(model, response, likedReelIds.getOrDefault(model.getId(), false));
    }

    @Override
    public ReelResponseDTO getReelById3(Long id, Boolean isLike) {
        ReelModel model = reelRepository.findById(id).orElse(null);
        if (model == null)
            return null;
        UserServiceProto.UserDTOResponse response = userGrpcClient.getUserDTOById(model.getUserId());
        return convertToReelResponseDTO(model, response, isLike);
    }

    @Override
    public ReelResponseDTO getReelById2(Long id) {
        ReelModel model = reelRepository.findById(id).orElse(null);
        if (model == null)
            return null;
        UserServiceProto.UserDTOResponse response = userGrpcClient.getUserDTOById(model.getUserId());
        return convertToReelResponseDTO(model, response, false);
    }

    @Override
    public String deleteReel(Long id) {
        ReelModel model = reelRepository.findById(id).orElseThrow(() -> new CustomException("Reel is not  exist", HttpStatus.BAD_REQUEST));
        securityUtil.checkOwner(model.getUserId());
        reelRepository.delete(model);

        String reelKey = "reel:" + id;
        String likesKey = "reel:likes:" + id;
        String commentsKey = "reel:comments:" + id;
        String feedKey = "feed:" + model.getUserId();

        redisTemplate.delete(reelKey);
        redisTemplate.delete(likesKey);
        redisTemplate.delete(commentsKey);

        redisTemplate.opsForZSet().remove(feedKey, id);
        return "Success";
    }

    @Override
    public String deleteReel2(Long id) {
        ReelModel model = reelRepository.findById(id).orElseThrow(() -> new CustomException("Reel is not  exist", HttpStatus.BAD_REQUEST));
        reelRepository.delete(model);

        String reelKey = "reel:" + id;
        String likesKey = "reel:likes:" + id;
        String commentsKey = "reel:comments:" + id;
        String feedKey = "feed:" + model.getUserId();

        redisTemplate.delete(reelKey);
        redisTemplate.delete(likesKey);
        redisTemplate.delete(commentsKey);

        redisTemplate.opsForZSet().remove(feedKey, id);
        return "Success";
    }

    @Override
    public String deletePost2(Long id) {
        PostModel model = postRepository.findById(id).orElseThrow(() -> new CustomException("Post is not  exist", HttpStatus.BAD_REQUEST));
        postRepository.delete(model);
        String postKey = "post:" + id;
        String likesKey = "post:likes:" + id;
        String commentsKey = "post:comments:" + id;
        String feedKey = "feed:" + model.getUserId();

        redisTemplate.delete(postKey);
        redisTemplate.delete(likesKey);
        redisTemplate.delete(commentsKey);
        redisTemplate.opsForZSet().remove(feedKey, id);
        return "Success";
    }

    @Override
    public PageModelResponse<PostResponseDTO> getPopularPosts(int page, int size, String hashtag) {
        Pageable pageable = PageRequest.of(page, size);
        Long id = securityUtil.getUserIdFromToken();
        UserServiceProto.UserDTOResponse currentUser = userGrpcClient.getUserDTOById(id);
        Page<PostModel> postPage = null;
        if (hashtag.isEmpty())
            postPage = postRepository.findPopularPublicPostsWithHashtagScore(pageable);
        else
            postPage = postRepository.findPopularPublicPostsByHashtag(hashtag, pageable);
        List<PostModel> posts = postPage.getContent();
        List<Long> postIds = posts.stream().map(PostModel::getId).toList();
        Map<Long, Boolean> likedPostIds = interactionGrpcClient.checkBatchLikesPost(currentUser.getId(), postIds);
        List<PostResponseDTO> postDTOs = postPage.getContent().stream()
                .map(post -> {
                    Boolean isLike = likedPostIds.getOrDefault(post.getId(), false);
                    return convertToPostResponseDTO(post, currentUser, isLike);
                })
                .toList();

        PageModelResponse<PostResponseDTO> response = new PageModelResponse<>();
        response.setContent(postDTOs);
        response.setPageSize(postPage.getNumber());
        response.setPageSize(postPage.getSize());
        response.setTotalElements(postPage.getTotalElements());
        response.setTotalPages(postPage.getTotalPages());
        response.setLast(postPage.isLast());

        return response;
    }

    @Override
    public List<PostResponseDTO> getPostsByIds(List<Long> postIds, Long viewerId) {
        List<PostResponseDTO> result = new ArrayList<>();
        Map<Long, Boolean> likedPostIds = interactionGrpcClient.checkBatchLikesPost(viewerId, postIds);
        for (Long id : postIds) {
            result.add(getPostById3(id, likedPostIds.getOrDefault(id, false)));
        }
        return result;
    }

    @Override
    public List<ReelResponseDTO> getReelsByIds(List<Long> postIds, Long viewerId) {
        List<ReelResponseDTO> result = new ArrayList<>();
        Map<Long, Boolean> likedPostIds = interactionGrpcClient.checkBatchLikesReel(viewerId, postIds);
        for (Long id : postIds) {
            result.add(getReelById3(id, likedPostIds.getOrDefault(id, false)));
        }
        return result;
    }

    PostResponseDTO convertToPostResponseDTO(PostModel model, UserServiceProto.UserDTOResponse userDto, Boolean isLike) {
        List<Long> tagIds = Optional.ofNullable(model.getTag())
                .filter(tag -> !tag.isBlank())
                .map(tag -> Arrays.stream(tag.split(","))
                        .filter(s -> !s.isBlank() && Long.parseLong(s) != model.getUserId())
                        .map(Long::parseLong)
                        .toList())
                .orElse(List.of());

        List<UserServiceProto.UserDTOResponse2> users = userGrpcClient.getUsersByIds(tagIds);

        List<UserTagDTO> userTagDTOList = users.stream()
                .map(u -> UserTagDTO.builder()
                        .userId(u.getId())
                        .userName(u.getUsername())
                        .build())
                .toList();

        Object likesStr = redisTemplate.opsForValue().get("post:likes:" + model.getId());
        Object commentsStr = redisTemplate.opsForValue().get("post:comments:" + model.getId());

        Long likes = likesStr != null ? Long.valueOf(likesStr.toString()) : 0L;
        Long comments = commentsStr != null ? Long.valueOf(commentsStr.toString()) : 0L;
        if (likes == 0L || comments == 0L) {
            likes = model.getLikeQuantity();
            comments = model.getCommentQuantity();
        }

        return PostResponseDTO.builder()
                .postId(model.getId())
                .userName(userDto.getUsername())
                .avatarUrl(userDto.getAvatar())
                .visibility(model.getVisibility().toString())
                .tag(model.getTag())
                .listUserTag(userTagDTOList)
                .caption(model.getCaption())
                .createdAt(model.getCreatedAt())
                .isActive(model.getIsActive())
                .isLike(isLike)
                .quantityLike(likes)
                .quantityComment(comments)
                .userId(model.getUserId())
                .mediaUrl(model.getPostMediaModels() != null ? model.getPostMediaModels().stream().map(PostMediaModel::getMediaUrl).toList() : List.of())
                .updatedAt(model.getUpdatedAt())
                .build();
    }

    ReelResponseDTO convertToReelResponseDTO(ReelModel model, UserServiceProto.UserDTOResponse userDto, Boolean isLike) {
        Object likesStr = redisTemplate.opsForValue().get("reel:likes:" + model.getId());
        Object commentsStr = redisTemplate.opsForValue().get("reel:comments:" + model.getId());

        Long likes = likesStr != null ? Long.valueOf(likesStr.toString()) : 0L;
        Long comments = commentsStr != null ? Long.valueOf(commentsStr.toString()) : 0L;
        return ReelResponseDTO.builder()
                .reelId(model.getId())
                .userName(userDto.getUsername())
                .avatarUrl(userDto.getAvatar())
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

    public void tagUserIntoPost(String oldTag, String users, Long currentUserId, Long postId) {
        List<Long> oldTagIds = Optional.ofNullable(oldTag)
                .filter(tag -> !tag.isBlank())
                .map(tag -> Arrays.stream(tag.split(","))
                        .filter(s -> !s.isBlank() && Long.parseLong(s) != currentUserId)
                        .map(Long::parseLong)
                        .toList())
                .orElse(List.of());

        List<Long> tagIds = Optional.ofNullable(users)
                .filter(tag -> !tag.isBlank())
                .map(tag -> Arrays.stream(tag.split(","))
                        .filter(s -> !s.isBlank() && Long.parseLong(s) != currentUserId)
                        .map(Long::parseLong)
                        .toList())
                .orElse(List.of());


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
