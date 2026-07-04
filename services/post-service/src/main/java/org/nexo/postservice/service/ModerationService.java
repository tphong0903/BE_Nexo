package org.nexo.postservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moderation.Moderation;
import org.nexo.grpc.interaction.InteractionServiceOuterClass;
import org.nexo.postservice.dto.MessageDTO;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.grpc.PostMediaServiceProto;
import org.nexo.postservice.model.*;
import org.nexo.postservice.repository.*;
import org.nexo.postservice.service.GrpcServiceImpl.client.AiModerationClient;
import org.nexo.postservice.service.GrpcServiceImpl.client.InteractionGrpcClient;
import org.nexo.postservice.service.impl.GeminiService;
import org.nexo.postservice.util.Enum.EMediaType;
import org.nexo.postservice.util.Enum.ENotificationType;
import org.nexo.postservice.util.Enum.EReportStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {
    private static final Long SYSTEM_ACTOR_ID = 0L;
    private static final String SYSTEM_REPORTER_NAME = "System";
    private static final String COMMUNITY_GUIDELINES_URL = "/community-guidelines";
    private static final String CONTENT_TYPE_POST = "POST";
    private static final String CONTENT_TYPE_REEL = "REEL";
    private static final String CONTENT_TYPE_COMMENT = "COMMENT";

    private final IPostRepository postRepository;
    private final IReelRepository reelRepository;
    private final IReportPostRepository reportPostRepository;
    private final IReportReelRepository reportReelRepository;
    private final IReportCommentRepository reportCommentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AiModerationClient aiModerationClient;
    private final IPostMediaService postMediaService;
    private final TransactionTemplate transactionTemplate;
    private final GeminiService geminiService;
    private final InteractionGrpcClient interactionGrpcClient;

    @Async("moderationTaskExecutor")
    public void triggerModerationAsync(String contentType, Long contentId, Long reporter) {
        try {
            if (CONTENT_TYPE_POST.equalsIgnoreCase(contentType)) {
                PostModel post = postRepository.findById(contentId).orElse(null);
                if (post == null) {
                    log.warn("[MODERATION] Post {} no longer exists, skipping async moderation", contentId);
                    return;
                }
                log.info("[MODERATION] Scheduled async moderation for post {}", contentId);
                moderatePost(post, reporter);
                return;
            }

            if (CONTENT_TYPE_REEL.equalsIgnoreCase(contentType)) {
                ReelModel reel = reelRepository.findById(contentId).orElse(null);
                if (reel == null) {
                    log.warn("[MODERATION] Reel {} no longer exists, skipping async moderation", contentId);
                    return;
                }
                log.info("[MODERATION] Scheduled async moderation for reel {}", contentId);
                moderateReel(reel, reporter);
                return;
            }

            if (CONTENT_TYPE_COMMENT.equalsIgnoreCase(contentType)) {
                var commentResponse = interactionGrpcClient.getCommentById(contentId);
                if (commentResponse == null) {
                    log.warn("[MODERATION] Comment {} no longer exists, skipping async moderation", contentId);
                    return;
                }
                log.info("[MODERATION] Scheduled async moderation for reel {}", contentId);
                moderateComment(commentResponse, reporter);
                return;
            }

            log.warn("[MODERATION] Unsupported content type {} for async moderation", contentType);
        } catch (Exception e) {
            log.error("[MODERATION] Failed to run async moderation for {} {}", contentType, contentId, e);
        }
    }

    private void moderatePost(PostModel post, Long reporter) {
        log.info("[MODERATION] Checking post {}", post.getId());
        ModerationResult violationResult;
        try {
            violationResult = checkPostViolation(post);
        } catch (Exception e) {
            log.error("[MODERATION] Failed to check post {}", post.getId(), e);
            return;
        }

        if (!violationResult.violated()) {
            ReportPostModel reportPostModel = reportPostRepository.findByUserIdAndPostModel_Id(reporter, post.getId());
            if (reportPostModel != null) {
                reportPostModel.setConfidence(violationResult.confidence());
                reportPostModel.setPredictAI(violationResult.label());
                reportPostRepository.save(reportPostModel);
            }
            return;
        }

        Long ownerId = post.getUserId();
        deactivateViolatedPost(post, violationResult, reporter);
        sendViolationNotification(ownerId, ENotificationType.POST_REMOVED);
    }

    private void moderateReel(ReelModel reel, Long reporter) {
        log.info("[MODERATION] Checking reel {}", reel.getId());
        ModerationResult violationResult;
        try {
            violationResult = checkReelViolation(reel);
        } catch (Exception e) {
            log.error("[MODERATION] Failed to check reel {}", reel.getId(), e);
            return;
        }

        if (!violationResult.violated()) {
            ReportReelModel reportReelModel = reportReelRepository.findByUserIdAndReelModel_Id(reporter, reel.getId());
            if (reportReelModel != null) {
                reportReelModel.setConfidence(violationResult.confidence());
                reportReelModel.setPredictAI(violationResult.label());
                reportReelRepository.save(reportReelModel);
            }
            return;
        }

        Long ownerId = reel.getUserId();
        deactivateViolatedReel(reel, violationResult, reporter);
        sendViolationNotification(ownerId, ENotificationType.POST_REMOVED);
    }

    private void moderateComment(InteractionServiceOuterClass.GetCommentByIdResponse comment, Long reporter) {
        log.info("[MODERATION] Checking comment {}", comment.getCommentId());
        ModerationResult violationResult;
        try {
            violationResult = checkCommentViolation(comment);
        } catch (Exception e) {
            log.error("[MODERATION] Failed to check reel {}", comment.getCommentId(), e);
            return;
        }

        ReportCommentModel reportCommentModel = reportCommentRepository.findByUserIdAndCommentId(reporter, comment.getCommentId());
        if (reportCommentModel != null) {
            reportCommentModel.setConfidence(violationResult.confidence());
            reportCommentModel.setPredictAI(violationResult.label());
            reportCommentRepository.save(reportCommentModel);
        }

        Long ownerId = comment.getUserId();
        deactivateViolatedComment(comment);
        sendViolationNotification(ownerId, ENotificationType.COMMENT_REMOVED);
    }

    private ModerationResult checkPostViolation(PostModel post) {
        Moderation.PredictionResponse prediction = aiModerationClient.checkText(post.getCaption());
        if (post.getCaption() != null && !post.getCaption().isBlank()) {
            if ("negative".equalsIgnoreCase(prediction.getLabel())) {
                log.info("[MODERATION] Caption violated");
                return ModerationResult.violated(
                        prediction.getLabel(),
                        (double) prediction.getConfidence(),
                        "Caption violated community guidelines"
                );
            }
        }

        List<String> imageUrls = postMediaService.findPostMediasOfPost(post.getId()).stream()
                .filter(media -> EMediaType.PICTURE.name().equals(media.getMediaType()))
                .map(PostMediaServiceProto.PostMediaRequestDTO::getMediaUrl)
                .toList();

        for (String imageUrl : imageUrls) {
            if (geminiService.isImageViolated(imageUrl)) {
                log.info("[MODERATION] Image violated");
                return ModerationResult.violated(
                        "negative",
                        1.0,
                        "Image violated community guidelines"
                );
            }
        }

        return ModerationResult.safe((double) prediction.getConfidence());
    }

    private ModerationResult checkReelViolation(ReelModel reel) {
        Moderation.PredictionResponse prediction = aiModerationClient.checkText(reel.getCaption());
        if (reel.getCaption() != null && !reel.getCaption().isBlank()) {
            if ("negative".equalsIgnoreCase(prediction.getLabel())) {
                log.info("[MODERATION] Reel caption violated");
                return ModerationResult.violated(
                        prediction.getLabel(),
                        (double) prediction.getConfidence(),
                        "Reel caption violated community guidelines"
                );
            }
        }
        return ModerationResult.safe((double) prediction.getConfidence());
    }

    private ModerationResult checkCommentViolation(InteractionServiceOuterClass.GetCommentByIdResponse comment) {
        Moderation.PredictionResponse prediction = aiModerationClient.checkText(comment.getContent());
        if (!comment.getContent().isBlank()) {
            if ("negative".equalsIgnoreCase(prediction.getLabel())) {
                log.info("[MODERATION] Comment caption violated");
                return ModerationResult.violated(
                        prediction.getLabel(),
                        (double) prediction.getConfidence(),
                        "Comment caption violated community guidelines"
                );
            }
        }
        return ModerationResult.safe((double) prediction.getConfidence());
    }

    private void deactivateViolatedPost(PostModel post, ModerationResult result, Long reporter) {
        Long postId = post.getId();
        Long ownerId = post.getUserId();

        transactionTemplate.executeWithoutResult(status -> {
            PostModel managedPost = postRepository.findById(postId)
                    .orElseThrow(() -> new CustomException("Post not found", HttpStatus.BAD_REQUEST));

            managedPost.setIsActive(false);
            postRepository.save(managedPost);
            createModerationReport(managedPost, result, reporter);

            clearPostCache(postId);
            redisTemplate.opsForZSet().remove("feed:" + ownerId, postId);
            redisTemplate.opsForZSet().remove("user_posts:" + ownerId, postId);
            redisTemplate.opsForZSet().remove("trending:posts", postId);
        });

        log.info("[MODERATION] Post {} deactivated and reported", postId);
    }

    private void deactivateViolatedReel(ReelModel reel, ModerationResult result, Long reporter) {
        Long reelId = reel.getId();
        Long ownerId = reel.getUserId();

        transactionTemplate.executeWithoutResult(status -> {
            ReelModel managedReel = reelRepository.findById(reelId)
                    .orElseThrow(() -> new CustomException("Reel not found", HttpStatus.BAD_REQUEST));

            managedReel.setIsActive(false);
            reelRepository.save(managedReel);
            createReelModerationReport(managedReel, result, reporter);

            clearReelCache(reelId);
            redisTemplate.opsForZSet().remove("feed:" + ownerId, reelId);
            redisTemplate.opsForZSet().remove("user_reels:" + ownerId, reelId);
            redisTemplate.opsForZSet().remove("trending:reels", reelId);
        });

        log.info("[MODERATION] Reel {} deactivated and reported", reelId);
    }

    private void deactivateViolatedComment(InteractionServiceOuterClass.GetCommentByIdResponse comment) {
        Long commentId = comment.getCommentId();

        transactionTemplate.executeWithoutResult(status -> {
            interactionGrpcClient.deleteCommentById(commentId);
        });

        log.info("[MODERATION] Comment {} deactivated and reported", commentId);
    }

    private void createModerationReport(PostModel post, ModerationResult result, Long reporter) {

        ReportPostModel report = ReportPostModel.builder()
                .userId(SYSTEM_ACTOR_ID)
                .postModel(post)
                .reason("Automatic moderation")
                .detail(result.reason())
                .reporterName(SYSTEM_REPORTER_NAME)
                .ownerPostName(post.getAuthorName())
                .reportStatus(EReportStatus.IN_REVIEW)
                .predictAI(result.label())
                .confidence(result.confidence())
                .note("Post was automatically deactivated by AI moderation.")
                .build();
        reportPostRepository.save(report);
        log.info("[MODERATION] Report created for post {}", post.getId());
    }

    private void createReelModerationReport(ReelModel reel, ModerationResult result, Long reporter) {
        ReportReelModel report = ReportReelModel.builder()
                .userId(SYSTEM_ACTOR_ID)
                .reelModel(reel)
                .reason("Automatic moderation")
                .detail(result.reason())
                .reporterName(SYSTEM_REPORTER_NAME)
                .ownerPostName(reel.getAuthorName())
                .reportStatus(EReportStatus.IN_REVIEW)
                .predictAI(result.label())
                .confidence(result.confidence())
                .note("Reel was automatically deactivated by AI moderation.")
                .build();
        reportReelRepository.save(report);
        log.info("[MODERATION] Report created for reel {}", reel.getId());
    }

    private void sendViolationNotification(Long ownerId, ENotificationType type) {
        try {
            MessageDTO messageDTO = MessageDTO.builder()
                    .actorId(SYSTEM_ACTOR_ID)
                    .recipientId(ownerId)
                    .notificationType(type.name())
                    .targetUrl(COMMUNITY_GUIDELINES_URL)
                    .build();
            kafkaTemplate.send("notification", messageDTO);
            log.info("[MODERATION] Notification sent to user {}", ownerId);
        } catch (Exception e) {
            log.error("[MODERATION] Failed to send violation notification to user {}", ownerId, e);
        }
    }

    private void clearPostCache(Long id) {
        redisTemplate.delete(Arrays.asList("post_dto_cache:" + id, "post:likes:" + id, "post:comments:" + id));
    }

    private void clearReelCache(Long id) {
        redisTemplate.delete(Arrays.asList("reel_dto_cache:" + id, "reel:likes:" + id, "reel:comments:" + id));
    }

    private record ModerationResult(boolean violated, String label, Double confidence, String reason) {
        private static ModerationResult safe(Double confidence) {
            return new ModerationResult(false, "normal", confidence, null);
        }

        private static ModerationResult violated(String label, Double confidence, String reason) {
            return new ModerationResult(true, label, confidence, reason);
        }
    }
}
