package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.interactionservice.cache.CacheKeys;
import org.nexo.interactionservice.cache.CachedPage;
import org.nexo.interactionservice.cache.InteractionCacheService;
import org.nexo.interactionservice.dto.MessageDTO;
import org.nexo.interactionservice.dto.UserActivityEvent;
import org.nexo.interactionservice.dto.response.FolloweeDTO;
import org.nexo.interactionservice.dto.response.PageModelResponse;
import org.nexo.interactionservice.exception.CustomException;
import org.nexo.interactionservice.mapper.FolloweeMapper;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.model.LikeCommentModel;
import org.nexo.interactionservice.model.LikeModel;
import org.nexo.interactionservice.model.UserPostScores;
import org.nexo.interactionservice.repository.ICommentRepository;
import org.nexo.interactionservice.repository.ILikeCommentRepository;
import org.nexo.interactionservice.repository.ILikeRepository;
import org.nexo.interactionservice.repository.IUserPostScoresRepository;
import org.nexo.interactionservice.service.ILikeService;
import org.nexo.interactionservice.util.Enum.ENotificationType;
import org.nexo.interactionservice.util.Enum.SecurityUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeServiceImpl implements ILikeService {

    private static final long SCORE_LIKE_POST_REEL = 2L;
    private static final long SCORE_LIKE_COMMENT = 1L;
    private final ILikeCommentRepository likeCommentRepository;
    private final ICommentRepository commentRepository;
    private final ILikeRepository likeRepository;
    private final IUserPostScoresRepository userPostScoresRepository;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;
    private final PostGrpcClient postGrpcClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final InteractionCacheService cacheService;

    @Override
    @Transactional
    public String saveLikeComment(Long id) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        LikeCommentModel likeCommentModel = likeCommentRepository.findByCommentModelIdAndAndUserId(id, currentUserId);
        CommentModel commentModel = commentRepository.findById(id)
                .orElseThrow(() -> new CustomException("Comment does not exist", HttpStatus.BAD_REQUEST));
        Long authorId = commentModel.getUserId();

        if (likeCommentModel != null) {
            likeCommentRepository.delete(likeCommentModel);
            cacheService.removeLikeStatus("comment", id, currentUserId);
            cacheService.incrementCounter("comment", id, "likes", -1L);
            updateAffinityScore(currentUserId, authorId, -SCORE_LIKE_COMMENT);
        } else {
            LikeCommentModel model = LikeCommentModel.builder()
                    .commentModel(commentModel)
                    .userId(currentUserId)
                    .build();
            likeCommentRepository.save(model);

            cacheService.addLikeStatus("comment", id, currentUserId);
            cacheService.incrementCounter("comment", id, "likes", 1L);

            updateAffinityScore(currentUserId, authorId, SCORE_LIKE_COMMENT);
            updateUserPostScore(currentUserId, commentModel.getPostId(), commentModel.getReelId(),
                    (double) SCORE_LIKE_COMMENT);

            if (!currentUserId.equals(commentModel.getUserId())) {
                String targetUrl = commentModel.getPostId() != null ? "/posts/" + commentModel.getPostId()
                        : "/reels/" + commentModel.getReelId();
                sendNotification(currentUserId, commentModel.getUserId(), ENotificationType.LIKE_COMMENT, targetUrl);
            }
        }

        cacheService.invalidateLikeList("comment", id);
        return "Success";
    }

    @Override
    @Transactional
    public String saveLikePost(Long id) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        LikeModel model = likeRepository.findByPostIdAndUserId(id, currentUserId);

        PostServiceOuterClass.PostResponse postResponse = postGrpcClient.getPostById(id);
        Long authorId = postResponse.getUserId();

        if (model != null) {
            likeRepository.delete(model);
            postGrpcClient.addLikeQuantityById(id, true, false);

            cacheService.removeLikeStatus("post", id, currentUserId);
            cacheService.incrementCounter("post", id, "likes", -1L);
            cacheService.incrementUserCounter(currentUserId, "likes", -1L);
            cacheService.incrementGlobalCounter("likes", -1L);

            updateAffinityScore(currentUserId, authorId, -SCORE_LIKE_POST_REEL);
            updateUserPostScore(currentUserId, id, null, (double) -SCORE_LIKE_POST_REEL);
        } else {
            model = LikeModel.builder().postId(id).userId(currentUserId).build();
            likeRepository.save(model);
            postGrpcClient.addLikeQuantityById(id, true, true);

            cacheService.addLikeStatus("post", id, currentUserId);
            cacheService.incrementCounter("post", id, "likes", 1L);
            cacheService.incrementUserCounter(currentUserId, "likes", 1L);
            cacheService.incrementGlobalCounter("likes", 1L);

            updateAffinityScore(currentUserId, authorId, SCORE_LIKE_POST_REEL);
            updateUserPostScore(currentUserId, id, null, (double) SCORE_LIKE_POST_REEL);

            if (!currentUserId.equals(authorId)) {
                sendNotification(currentUserId, authorId, ENotificationType.LIKE_POST, "/posts/" + id);
            }

            String authorName = "ai đó";
            try {
                org.nexo.grpc.user.UserServiceProto.UserDTOResponse author = userGrpcClient.getUserDTOById(authorId);
                authorName = author.getFullName();
            } catch (Exception e) {
                log.warn("Failed to fetch author name for POST_LIKED event: {}", e.getMessage());
            }

            String metadata = String.format("{\"source\":\"interaction-service\", \"authorName\":\"%s\", \"url\":\"/posts/%d\"}", 
                authorName.replace("\"", "\\\""), id);

            UserActivityEvent activityEvent = UserActivityEvent.builder()
                    .eventType("POST_LIKED")
                    .userId(currentUserId)
                    .targetId(id)
                    .targetType("POST")
                    .occurredAt(java.time.Instant.now())
                    .metadata(metadata)
                    .build();
            kafkaTemplate.send("user-events", String.valueOf(currentUserId), activityEvent);
            kafkaTemplate.send("user-activity-events", String.valueOf(currentUserId), activityEvent);
        }

        cacheService.invalidateLikeList("post", id);
        return "Success";
    }

    @Override
    @Transactional
    public String saveLikeReel(Long id) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        LikeModel model = likeRepository.findByReelIdAndUserId(id, currentUserId);

        PostServiceOuterClass.ReelResponse reelResponse = postGrpcClient.getReelById(id);
        Long authorId = reelResponse.getUserId();

        if (model != null) {
            likeRepository.delete(model);
            postGrpcClient.addLikeQuantityById(id, false, false);

            cacheService.removeLikeStatus("reel", id, currentUserId);
            cacheService.incrementCounter("reel", id, "likes", -1L);
            cacheService.incrementUserCounter(currentUserId, "likes", -1L);
            cacheService.incrementGlobalCounter("likes", -1L);

            updateAffinityScore(currentUserId, authorId, -SCORE_LIKE_POST_REEL);
            updateUserPostScore(currentUserId, null, id, (double) -SCORE_LIKE_POST_REEL);
        } else {
            model = LikeModel.builder().reelId(id).userId(currentUserId).build();
            likeRepository.save(model);
            postGrpcClient.addLikeQuantityById(id, false, true);

            cacheService.addLikeStatus("reel", id, currentUserId);
            cacheService.incrementCounter("reel", id, "likes", 1L);
            cacheService.incrementUserCounter(currentUserId, "likes", 1L);
            cacheService.incrementGlobalCounter("likes", 1L);

            updateAffinityScore(currentUserId, authorId, SCORE_LIKE_POST_REEL);
            updateUserPostScore(currentUserId, null, id, (double) SCORE_LIKE_POST_REEL);

            if (!currentUserId.equals(reelResponse.getUserId())) {
                sendNotification(currentUserId, reelResponse.getUserId(), ENotificationType.LIKE_REEL, "/reels/" + id);
            }
        }

        cacheService.invalidateLikeList("reel", id);
        return "Success";
    }

    @Override
    public PageModelResponse<FolloweeDTO> getLikePostDetail(Long id, int pageNo, int pageSize) {
        Long currentUserId = securityUtil.getUserIdFromToken();

        long version = cacheService.likesVersion("post", id);

        String cacheKey = CacheKeys.likesPage("post", id, version, pageNo, pageSize);

        CachedPage<Long> likedUserIds = cacheService.getOrLoadPageIds(cacheKey, cacheService.likePageTtl(), () -> {
            Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
            return cacheService.toLikeUserIdPage(likeRepository.findByPostId(id, pageable));
        });

        boolean hasLiked = cacheService.isLiked("post", id, currentUserId);
        if (!hasLiked) {
            hasLiked = likeRepository.existsByPostIdAndUserId(id, currentUserId);
        }
        return buildLikeDetailResponse(likedUserIds, currentUserId, hasLiked);
    }

    @Override
    public PageModelResponse<FolloweeDTO> getLikeReelDetail(Long id, int pageNo, int pageSize) {
        Long currentUserId = securityUtil.getUserIdFromToken();

        long version = cacheService.likesVersion("reel", id);
        String cacheKey = CacheKeys.likesPage("reel", id, version, pageNo, pageSize);
        CachedPage<Long> likedUserIds = cacheService.getOrLoadPageIds(cacheKey, cacheService.likePageTtl(), () -> {
            Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
            return cacheService.toLikeUserIdPage(likeRepository.findByReelId(id, pageable));
        });

        boolean hasLiked = cacheService.isLiked("reel", id, currentUserId);
        if (!hasLiked) {
            hasLiked = likeRepository.existsByReelIdAndUserId(id, currentUserId);
        }
        return buildLikeDetailResponse(likedUserIds, currentUserId, hasLiked);
    }

    @Override
    public PageModelResponse<FolloweeDTO> getLikeCommentDetail(Long id, int pageNo, int pageSize) {
        Long currentUserId = securityUtil.getUserIdFromToken();

        long version = cacheService.likesVersion("comment", id);
        String cacheKey = CacheKeys.likesPage("comment", id, version, pageNo, pageSize);
        CachedPage<Long> likedUserIds = cacheService.getOrLoadPageIds(cacheKey, cacheService.likePageTtl(), () -> {
            Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
            return cacheService.toCommentLikeUserIdPage(likeCommentRepository.findByCommentModelId(id, pageable));
        });

        boolean hasLiked = cacheService.isLiked("comment", id, currentUserId);
        if (!hasLiked) {
            hasLiked = likeCommentRepository.existsByCommentModelIdAndUserId(id, currentUserId);
        }
        return buildLikeDetailResponse(likedUserIds, currentUserId, hasLiked);
    }

    private void sendNotification(Long actorId, Long recipientId, ENotificationType type, String targetUrl) {
        MessageDTO messageDTO = MessageDTO.builder()
                .actorId(actorId)
                .recipientId(recipientId)
                .notificationType(type.name())
                .targetUrl(targetUrl)
                .build();
        kafkaTemplate.send("notification", messageDTO);
    }

    private PageModelResponse<FolloweeDTO> buildLikeDetailResponse(
            CachedPage<Long> pageData, Long currentUserId, boolean hasLikedCurrent) {

        List<Long> likedUserIds = pageData.getContent();
        if (likedUserIds.isEmpty()) {
            return buildEmptyPageResponse(pageData.getPageNo(), pageData.getPageSize());
        }

        List<UserServiceProto.UserDTOResponse3> listUserData = userGrpcClient.getLikeUsersByIds(currentUserId,
                likedUserIds);

        List<FolloweeDTO> followeeDTOs = new ArrayList<>(FolloweeMapper.toFolloweeDTOList(listUserData));

        if (hasLikedCurrent) {
            UserServiceProto.UserDTOResponse currentUser = userGrpcClient.getUserDTOById(currentUserId);
            FolloweeDTO currentUserDTO = FolloweeMapper.toFolloweeDTO2(currentUser);

            followeeDTOs.removeIf(dto -> dto.getUserId().equals(currentUserDTO.getUserId()));
            followeeDTOs.add(0, currentUserDTO);
        }

        PageModelResponse<FolloweeDTO> pageResponse = new PageModelResponse<>();
        pageResponse.setContent(followeeDTOs);
        pageResponse.setPageNo(pageData.getPageNo());
        pageResponse.setPageSize(pageData.getPageSize());
        pageResponse.setTotalElements(pageData.getTotalElements());
        pageResponse.setTotalPages(pageData.getTotalPages());
        pageResponse.setLast(pageData.isLast());

        return pageResponse;
    }

    private PageModelResponse<FolloweeDTO> buildEmptyPageResponse(int pageNo, int pageSize) {
        PageModelResponse<FolloweeDTO> response = new PageModelResponse<>();
        response.setContent(new ArrayList<>());
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setTotalElements(0L);
        response.setTotalPages(0);
        response.setLast(true);
        return response;
    }

    private void updateAffinityScore(Long followerId, Long authorId, long scoreDelta) {
        if (followerId.equals(authorId))
            return;

        String affinityKey = "affinity:" + followerId;
        redisTemplate.opsForHash().increment(affinityKey, String.valueOf(authorId), scoreDelta);
        log.info("Updated affinity score for follower {} -> author {} by delta {}", followerId, authorId, scoreDelta);
    }

    private void updateUserPostScore(Long userId, Long postId, Long reelId, Double scoreDelta) {
        UserPostScores userPostScores = null;
        if (postId != null) {
            userPostScores = userPostScoresRepository.findByUserIdAndPostId(userId, postId);
        } else if (reelId != null) {
            userPostScores = userPostScoresRepository.findByUserIdAndReelId(userId, reelId);
        }

        if (userPostScores == null) {
            userPostScores = UserPostScores.builder()
                    .userId(userId)
                    .postId(postId)
                    .reelId(reelId)
                    .scores(scoreDelta > 0 ? scoreDelta : 0.0)
                    .build();
        } else {
            double newScore = (userPostScores.getScores() != null ? userPostScores.getScores() : 0.0) + scoreDelta;
            userPostScores.setScores(Math.max(newScore, 0.0));
        }

        userPostScoresRepository.save(userPostScores);
    }

}
