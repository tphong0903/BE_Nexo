package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.interactionservice.dto.MessageDTO;
import org.nexo.interactionservice.dto.UserActivityEvent;
import org.nexo.interactionservice.dto.request.CommentDto;
import org.nexo.interactionservice.dto.response.ListCommentResponse;
import org.nexo.interactionservice.exception.CustomException;
import org.nexo.interactionservice.mapper.CommentMapper;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.model.UserPostScores;
import org.nexo.interactionservice.repository.ICommentRepository;
import org.nexo.interactionservice.repository.IUserPostScoresRepository;
import org.nexo.interactionservice.service.ICommentMentionService;
import org.nexo.interactionservice.service.ICommentService;
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

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements ICommentService {
    private static final long SCORE_COMMENT = 3L;
    private final ICommentRepository commentRepository;
    private final ICommentMentionService commentMentionService;
    private final IUserPostScoresRepository userPostScoresRepository;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;
    private final PostGrpcClient postGrpcClient;
    private final CommentMapper commentMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public String saveComment(CommentDto dto) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        if (!currentUserId.equals(dto.getUserId())) {
            throw new CustomException("Don't allow to comment for another user", HttpStatus.FORBIDDEN);
        }

        CommentModel model;
        boolean isNewComment = (dto.getId() == null || dto.getId() == 0);

        if (!isNewComment) {
            model = commentRepository.findById(dto.getId())
                    .orElseThrow(() -> new CustomException("Comment does not exist", HttpStatus.BAD_REQUEST));
            model.setContent(dto.getContent());
            commentRepository.save(model);

            redisTemplate.delete("comment_cache:" + model.getId());
            invalidateListCache(model);
            if (dto.getListMentionUserId() != null) {
                commentMentionService.syncMentionComment(dto.getListMentionUserId(), model);
            }
            return "Success";
        }

        model = CommentModel.builder()
                .content(dto.getContent())
                .userId(dto.getUserId())
                .build();

        boolean isPost = (dto.getPostId() != null && dto.getPostId() != 0);
        if (isPost) {
            model.setPostId(dto.getPostId());
        } else {
            model.setReelId(dto.getReelId());
        }

        if (dto.getParentId() != null && dto.getParentId() != 0) {
            CommentModel parent = commentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new CustomException("Parent comment does not exist", HttpStatus.BAD_REQUEST));
            model.setParentComment(parent);
        }

        commentRepository.save(model);
        invalidateListCache(model);
        redisTemplate.opsForValue().setIfAbsent("global:likes:total", 0L);
        redisTemplate.opsForValue().increment("user:" + currentUserId + ":comments:total");

        if (dto.getListMentionUserId() != null && !dto.getListMentionUserId().isEmpty()) {
            commentMentionService.syncMentionComment(dto.getListMentionUserId(), model);
        }

        Long authorId;
        String notificationType;
        String url;

        if (isPost) {
            postGrpcClient.addCommentQuantityById(model.getPostId(), true, true);
            notificationType = ENotificationType.COMMENT_POST.name();
            authorId = postGrpcClient.getPostById(model.getPostId()).getUserId();
            url = "/posts/" + model.getPostId();
        } else {
            postGrpcClient.addCommentQuantityById(model.getReelId(), false, true);
            notificationType = ENotificationType.COMMENT_REEL.name();
            authorId = postGrpcClient.getReelById(model.getReelId()).getUserId();
            url = "/reels/" + model.getReelId();
        }

        updateAffinityScore(currentUserId, authorId, SCORE_COMMENT);
        updateUserPostScore(currentUserId, model.getPostId(), model.getReelId(), (double) SCORE_COMMENT);

        if (!currentUserId.equals(authorId)) {
            MessageDTO messageDTO = MessageDTO.builder()
                    .actorId(currentUserId)
                    .recipientId(authorId)
                    .notificationType(notificationType)
                    .targetUrl(url)
                    .build();
            kafkaTemplate.send("notification", messageDTO);

            String authorName = "ai đó";
            try {
                org.nexo.grpc.user.UserServiceProto.UserDTOResponse author = userGrpcClient.getUserDTOById(authorId);
                authorName = author.getFullName();
            } catch (Exception e) {
                log.warn("Failed to fetch author name for COMMENT_CREATED event: {}", e.getMessage());
            }

            Long sourceId = model.getPostId() != null && model.getPostId() != 0 ? model.getPostId() : model.getReelId();
            String typeStr = model.getPostId() != null && model.getPostId() != 0 ? "post" : "reel";
            String urlTypeStr = model.getPostId() != null && model.getPostId() != 0 ? "posts" : "reels";
            String metadata = String.format("{\"source\":\"interaction-service\", \"authorName\":\"%s\", \"url\":\"/%s/%d\"}", 
                authorName.replace("\"", "\\\""), urlTypeStr, sourceId);

            UserActivityEvent activityEvent = UserActivityEvent.builder()
                    .eventType("COMMENT_CREATED")
                    .userId(dto.getUserId())
                    .targetId(authorId)
                    .targetType(typeStr.toUpperCase())
                    .occurredAt(Instant.now())
                    .metadata(metadata)
                    .build();
            kafkaTemplate.send("user-activity-events", String.valueOf(dto.getUserId()), activityEvent);
            kafkaTemplate.send("user-events", String.valueOf(dto.getUserId()), activityEvent);

        }

        return "Success";
    }

    @Override
    @Transactional
    public String deleteComment(Long id) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        CommentModel model = commentRepository.findById(id)
                .orElseThrow(() -> new CustomException("Comment does not exist", HttpStatus.BAD_REQUEST));

        Long postAuthorId = (model.getPostId() != null)
                ? postGrpcClient.getPostById(model.getPostId()).getUserId()
                : postGrpcClient.getReelById(model.getReelId()).getUserId();

        boolean isOwnerOfComment = currentUserId.equals(model.getUserId());
        boolean isPostOwner = currentUserId.equals(postAuthorId);

        if (!isOwnerOfComment && !isPostOwner) {
            throw new CustomException("Don't have permission to delete this comment", HttpStatus.FORBIDDEN);
        }

        commentRepository.delete(model);

        redisTemplate.delete("comment_cache:" + id);
        invalidateListCache(model);
        redisTemplate.opsForValue().decrement("global:comments:total");
        redisTemplate.opsForValue().decrement("user:" + model.getUserId() + ":comments:total");

        if (model.getPostId() != null) {
            postGrpcClient.addCommentQuantityById(model.getPostId(), true, false);
        } else if (model.getReelId() != null) {
            postGrpcClient.addCommentQuantityById(model.getReelId(), false, false);
        }

        updateAffinityScore(model.getUserId(), postAuthorId, -SCORE_COMMENT);
        updateUserPostScore(model.getUserId(), model.getPostId(), model.getReelId(), (double) -SCORE_COMMENT);

        return "Success";
    }

    @Override
    public ListCommentResponse getCommentOfPost(Long postId, int pageNo, int pageSize) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        long version = getCacheVersion("post", postId);
        String cacheKey = String.format("cache:comments:post:%d:v:%d:p:%d:s:%d:u:%d",
                postId, version, pageNo, pageSize, currentUserId);

        ListCommentResponse cachedResponse = (ListCommentResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        PostServiceOuterClass.PostResponse post = postGrpcClient.getPostById(postId);

        checkVisibilityAccess(post.getUserId(), currentUserId);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").ascending());
        Page<CommentModel> commentsPage = commentRepository.findByPostIdAndParentComment(postId, pageable, null);

        ListCommentResponse response = commentMapper.toListResponse(postId, commentsPage, currentUserId);

        redisTemplate.opsForValue().set(cacheKey, response, 10, TimeUnit.MINUTES);
        return response;
    }

    @Override
    public ListCommentResponse getCommentOfReel(Long reelId, int pageNo, int pageSize) {
        Long currentUserId = securityUtil.getUserIdFromToken();

        long version = getCacheVersion("reel", reelId);
        String cacheKey = String.format("cache:comments:reel:%d:v:%d:p:%d:s:%d:u:%d",
                reelId, version, pageNo, pageSize, currentUserId);
        ListCommentResponse cachedResponse = (ListCommentResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        PostServiceOuterClass.ReelResponse reel = postGrpcClient.getReelById(reelId);
        checkVisibilityAccess(reel.getUserId(), currentUserId);

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").ascending());
        Page<CommentModel> commentsPage = commentRepository.findByReelIdAndParentComment(reelId, pageable, null);

        ListCommentResponse response = commentMapper.toListResponse(reelId, commentsPage, currentUserId);
        redisTemplate.opsForValue().set(cacheKey, response, 10, TimeUnit.MINUTES);
        return response;
    }

    @Override
    public ListCommentResponse getReplies(Long commentId, int pageNo, int pageSize) {
        Long currentUserId = securityUtil.getUserIdFromToken();
        // long version = getCacheVersion("reply", commentId);
        // String cacheKey =
        // String.format("cache:comments:reply:%d:v:%d:p:%d:s:%d:u:%d",
        // commentId, version, pageNo, pageSize, currentUserId);
        //
        // ListCommentResponse cachedResponse = (ListCommentResponse)
        // redisTemplate.opsForValue().get(cacheKey);
        // if (cachedResponse != null) {
        // return cachedResponse;
        // }

        CommentModel parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException("Comment does not exist", HttpStatus.BAD_REQUEST));

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").ascending());
        Page<CommentModel> repliesPage = commentRepository.findByParentCommentId(commentId, pageable);

        Long sourceId = (parentComment.getPostId() != null) ? parentComment.getPostId() : parentComment.getReelId();

        // redisTemplate.opsForValue().set(cacheKey, response, 10, TimeUnit.MINUTES);
        return commentMapper.toListResponse(sourceId, repliesPage, currentUserId);
    }

    private void updateAffinityScore(Long followerId, Long authorId, long scoreDelta) {
        if (followerId.equals(authorId))
            return;

        String affinityKey = "affinity:" + followerId;
        redisTemplate.opsForHash().increment(affinityKey, String.valueOf(authorId), scoreDelta);
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

    private void checkVisibilityAccess(Long authorId, Long viewerId) {
        if (authorId.equals(viewerId))
            return;

        UserServiceProto.CheckFollowResponse followCheck = userGrpcClient.checkFollow(viewerId, authorId);
        if (followCheck.getIsPrivate() && !followCheck.getIsFollow()) {
            throw new CustomException("This account is private. Follow to view comments.", HttpStatus.FORBIDDEN);
        }
    }

    private long getCacheVersion(String prefix, Long id) {
        Object v = redisTemplate.opsForValue().get(prefix + ":version:" + id);
        return v != null ? ((Number) v).longValue() : 1L;
    }

    private void incrementCacheVersion(String prefix, Long id) {
        redisTemplate.opsForValue().increment(prefix + ":version:" + id);
    }

    private void invalidateListCache(CommentModel model) {
        if (model.getPostId() != null) {
            incrementCacheVersion("post", model.getPostId());
        }
        if (model.getReelId() != null) {
            incrementCacheVersion("reel", model.getReelId());
        }
        if (model.getParentComment() != null) {
            incrementCacheVersion("reply", model.getParentComment().getId());
        }
    }
}