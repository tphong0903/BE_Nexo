package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
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
import org.springframework.data.domain.Page;
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
import java.util.concurrent.TimeUnit;

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
            redisTemplate.opsForSet().remove("comment:" + id + ":likes", String.valueOf(currentUserId));
            updateAffinityScore(currentUserId, authorId, -SCORE_LIKE_COMMENT);
        } else {
            LikeCommentModel model = LikeCommentModel.builder()
                    .commentModel(commentModel)
                    .userId(currentUserId)
                    .build();
            likeCommentRepository.save(model);

            redisTemplate.opsForSet().add("comment:" + id + ":likes", String.valueOf(currentUserId));

            updateAffinityScore(currentUserId, authorId, SCORE_LIKE_COMMENT);
            updateUserPostScore(currentUserId, commentModel.getPostId(), commentModel.getReelId(),
                    (double) SCORE_LIKE_COMMENT);

            if (!currentUserId.equals(commentModel.getUserId())) {
                String targetUrl = commentModel.getPostId() != null ? "/posts/" + commentModel.getPostId()
                        : "/reels/" + commentModel.getReelId();
                sendNotification(currentUserId, commentModel.getUserId(), ENotificationType.LIKE_COMMENT, targetUrl);
            }
        }

        incrementCacheVersion("comment", id);
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

            redisTemplate.opsForSet().remove("post:" + id + ":likes", String.valueOf(currentUserId));
            redisTemplate.opsForValue().decrement("global:likes:total");
            redisTemplate.opsForValue().decrement("user:" + currentUserId + ":likes:total");

            updateAffinityScore(currentUserId, authorId, -SCORE_LIKE_POST_REEL);
            updateUserPostScore(currentUserId, id, null, (double) -SCORE_LIKE_POST_REEL);
        } else {
            model = LikeModel.builder().postId(id).userId(currentUserId).build();
            likeRepository.save(model);
            postGrpcClient.addLikeQuantityById(id, true, true);

            redisTemplate.opsForSet().add("post:" + id + ":likes", String.valueOf(currentUserId));
            redisTemplate.opsForValue().increment("global:likes:total");
            redisTemplate.opsForValue().increment("user:" + currentUserId + ":likes:total");

            updateAffinityScore(currentUserId, authorId, SCORE_LIKE_POST_REEL);
            updateUserPostScore(currentUserId, id, null, (double) SCORE_LIKE_POST_REEL);

            if (!currentUserId.equals(authorId)) {
                sendNotification(currentUserId, authorId, ENotificationType.LIKE_POST, "/posts/" + id);
            }

            UserActivityEvent activityEvent = UserActivityEvent.builder()
                    .eventType("POST_LIKED")
                    .userId(currentUserId)
                    .targetId(id)
                    .targetType("POST")
                    .occurredAt(java.time.Instant.now())
                    .metadata("{\"source\":\"interaction-service\"}")
                    .build();
            kafkaTemplate.send("user-events", String.valueOf(currentUserId), activityEvent);
        }

        incrementCacheVersion("post", id);
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

            redisTemplate.opsForSet().remove("reel:" + id + ":likes", String.valueOf(currentUserId));
            redisTemplate.opsForValue().decrement("global:likes:total");
            redisTemplate.opsForValue().decrement("user:" + currentUserId + ":likes:total");

            updateAffinityScore(currentUserId, authorId, -SCORE_LIKE_POST_REEL);
            updateUserPostScore(currentUserId, null, id, (double) -SCORE_LIKE_POST_REEL);
        } else {
            model = LikeModel.builder().reelId(id).userId(currentUserId).build();
            likeRepository.save(model);
            postGrpcClient.addLikeQuantityById(id, false, true);

            redisTemplate.opsForSet().add("reel:" + id + ":likes", String.valueOf(currentUserId));
            redisTemplate.opsForValue().increment("global:likes:total");
            redisTemplate.opsForValue().increment("user:" + currentUserId + ":likes:total");

            updateAffinityScore(currentUserId, authorId, SCORE_LIKE_POST_REEL);
            updateUserPostScore(currentUserId, null, id, (double) SCORE_LIKE_POST_REEL);

            if (!currentUserId.equals(reelResponse.getUserId())) {
                sendNotification(currentUserId, reelResponse.getUserId(), ENotificationType.LIKE_REEL, "/reels/" + id);
            }
        }

        incrementCacheVersion("reel", id);
        String keyloakId = securityUtil.getKeyloakId();
        UserServiceProto.UserDto response = userGrpcClient.getUserByKeycloakId(keyloakId);
        LikeModel postLikeModel = likeRepository.findByPostIdAndUserId(id, response.getUserId());
        try {
            if (postLikeModel != null) {
                likeRepository.delete(postLikeModel);
                postGrpcClient.addLikeQuantityById(id, true, false);
            } else {
                postLikeModel = LikeModel.builder()
                        .postId(id)
                        .userId(response.getUserId())
                        .build();
                likeRepository.save(postLikeModel);
                postGrpcClient.addLikeQuantityById(id, true, true);
                PostServiceOuterClass.PostResponse postResponse = postGrpcClient.getPostById(id);
                MessageDTO messageDTO = MessageDTO.builder()
                        .actorId(response.getUserId())
                        .recipientId(postResponse.getUserId())
                        .notificationType(String.valueOf(ENotificationType.LIKE_POST))
                        .targetUrl("/posts/" + id)
                        .build();
                kafkaTemplate.send("notification", messageDTO);

                UserActivityEvent activityEvent = UserActivityEvent.builder()
                        .eventType("POST_LIKED")
                        .userId(response.getUserId())
                        .targetId(id)
                        .targetType("POST")
                        .occurredAt(java.time.Instant.now())
                        .metadata("{\"source\":\"interaction-service\"}")
                        .build();
                kafkaTemplate.send("user-events", String.valueOf(response.getUserId()), activityEvent);
            }
        } catch (Exception e) {
            throw new CustomException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return "Success";
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageModelResponse<FolloweeDTO> getLikePostDetail(Long id, int pageNo, int pageSize) {
        Long currentUserId = securityUtil.getUserIdFromToken();

        long version = getCacheVersion("post", id);
        String cacheKey = String.format("cache:likes:post:%d:v:%d:p:%d:s:%d:u:%d",
                id, version, pageNo, pageSize, currentUserId);

        PageModelResponse<FolloweeDTO> cachedResponse = (PageModelResponse<FolloweeDTO>) redisTemplate.opsForValue()
                .get(cacheKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        // 2. Không có Cache -> Query DB & gRPC
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        Page<LikeModel> likePage = likeRepository.findByPostId(id, pageable);
        List<Long> likedUserIds = likePage.getContent().stream().map(LikeModel::getUserId).toList();

        boolean hasLiked = likeRepository.existsByPostIdAndUserId(id, currentUserId);

        PageModelResponse<FolloweeDTO> response = buildLikeDetailResponse(likePage, likedUserIds, currentUserId,
                hasLiked);

        redisTemplate.opsForValue().set(cacheKey, response, 15, TimeUnit.MINUTES);
        return response;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageModelResponse<FolloweeDTO> getLikeReelDetail(Long id, int pageNo, int pageSize) {
        Long currentUserId = securityUtil.getUserIdFromToken();

        long version = getCacheVersion("reel", id);
        String cacheKey = String.format("cache:likes:reel:%d:v:%d:p:%d:s:%d:u:%d",
                id, version, pageNo, pageSize, currentUserId);

        PageModelResponse<FolloweeDTO> cachedResponse = (PageModelResponse<FolloweeDTO>) redisTemplate.opsForValue()
                .get(cacheKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        Page<LikeModel> likePage = likeRepository.findByReelId(id, pageable);
        List<Long> likedUserIds = likePage.getContent().stream().map(LikeModel::getUserId).toList();

        boolean hasLiked = likeRepository.existsByReelIdAndUserId(id, currentUserId);

        PageModelResponse<FolloweeDTO> response = buildLikeDetailResponse(likePage, likedUserIds, currentUserId,
                hasLiked);

        redisTemplate.opsForValue().set(cacheKey, response, 15, TimeUnit.MINUTES);
        return response;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PageModelResponse<FolloweeDTO> getLikeCommentDetail(Long id, int pageNo, int pageSize) {
        Long currentUserId = securityUtil.getUserIdFromToken();

        long version = getCacheVersion("comment", id);
        String cacheKey = String.format("cache:likes:comment:%d:v:%d:p:%d:s:%d:u:%d",
                id, version, pageNo, pageSize, currentUserId);

        PageModelResponse<FolloweeDTO> cachedResponse = (PageModelResponse<FolloweeDTO>) redisTemplate.opsForValue()
                .get(cacheKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());
        Page<LikeCommentModel> likePage = likeCommentRepository.findByCommentModelId(id, pageable);
        List<Long> likedUserIds = likePage.getContent().stream().map(LikeCommentModel::getUserId).toList();

        boolean hasLiked = likeCommentRepository.existsByCommentModelIdAndUserId(id, currentUserId);

        PageModelResponse<FolloweeDTO> response = buildLikeDetailResponse(likePage, likedUserIds, currentUserId,
                hasLiked);

        redisTemplate.opsForValue().set(cacheKey, response, 15, TimeUnit.MINUTES);
        return response;
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
            Page<?> pageData, List<Long> likedUserIds, Long currentUserId, boolean hasLikedCurrent) {

        if (likedUserIds.isEmpty()) {
            return buildEmptyPageResponse(pageData.getNumber(), pageData.getSize());
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
        pageResponse.setPageNo(pageData.getNumber());
        pageResponse.setPageSize(pageData.getSize());
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

    private long getCacheVersion(String prefix, Long id) {
        Object v = redisTemplate.opsForValue().get(prefix + ":like_version:" + id);
        return v != null ? ((Number) v).longValue() : 1L;
    }

    private void incrementCacheVersion(String prefix, Long id) {
        redisTemplate.opsForValue().increment(prefix + ":like_version:" + id);
    }
}