package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moderation.Moderation;
import org.nexo.grpc.interaction.InteractionServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.dto.response.*;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.*;
import org.nexo.postservice.repository.*;
import org.nexo.postservice.service.GrpcServiceImpl.client.AiModerationClient;
import org.nexo.postservice.service.GrpcServiceImpl.client.InteractionGrpcClient;
import org.nexo.postservice.service.GrpcServiceImpl.client.UserGrpcClient;
import org.nexo.postservice.service.IReportService;
import org.nexo.postservice.util.Enum.EReportStatus;
import org.nexo.postservice.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements IReportService {
    private final IReportPostRepository reportPostRepository;
    private final IReportReelRepository reportReelRepository;
    private final IReportCommentRepository reportCommentRepository;
    private final IPostRepository postRepository;
    private final IReelRepository reelRepository;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;
    private final InteractionGrpcClient interactionGrpcClient;
    private final AiModerationClient aiModerationClient;


    private Map<Long, UserServiceProto.UserDTOResponse2> getUserMap(List<Long> ids) {
        return userGrpcClient.getUsersByIds(ids).stream()
                .collect(Collectors.toMap(UserServiceProto.UserDTOResponse2::getId, u -> u, (a, b) -> a));
    }

    private void validateReportPermission(Long reporterId, Long ownerId) {
        if (reporterId.equals(ownerId)) return;
        UserServiceProto.CheckFollowResponse followResponse = userGrpcClient.checkFollow(reporterId, ownerId);
        if (followResponse.getIsPrivate() && !followResponse.getIsFollow()) {
            throw new CustomException("You are not allowed to access this private content", HttpStatus.FORBIDDEN);
        }
    }


    @Override
    @Transactional
    public String reportPost(Long id, String reason, String detail) {
        Long userId = securityUtil.getUserIdFromToken();
        if (reportPostRepository.existsByUserIdAndPostModel_Id(userId, id)) {
            throw new CustomException("You already reported this post", HttpStatus.BAD_REQUEST);
        }

        PostModel post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException("Post not found", HttpStatus.NOT_FOUND));

        validateReportPermission(userId, post.getUserId());
        var userMap = getUserMap(List.of(post.getUserId(), userId));

        ReportPostModel report = ReportPostModel.builder()
                .userId(userId)
                .postModel(post)
                .reason(reason)
                .detail(detail)
                .reporterName(userMap.get(userId).getUsername())
                .ownerPostName(userMap.get(post.getUserId()).getUsername())
                .reportStatus(EReportStatus.PENDING)
                .build();

        reportPostRepository.save(report);
        callAIServiceToCheck(post.getCaption(), "POST", report.getId());
        return "Success";
    }

    @Override
    @Transactional
    public String reportReel(Long id, String reason, String detail) {
        Long userId = securityUtil.getUserIdFromToken();
        if (reportReelRepository.existsByUserIdAndReelModel_Id(userId, id)) {
            throw new CustomException("You already reported this reel", HttpStatus.BAD_REQUEST);
        }

        ReelModel reel = reelRepository.findById(id)
                .orElseThrow(() -> new CustomException("Reel not found", HttpStatus.NOT_FOUND));

        validateReportPermission(userId, reel.getUserId());
        var userMap = getUserMap(List.of(reel.getUserId(), userId));

        ReportReelModel report = ReportReelModel.builder()
                .userId(userId)
                .reelModel(reel)
                .reason(reason)
                .detail(detail)
                .reporterName(userMap.get(userId).getUsername())
                .ownerPostName(userMap.get(reel.getUserId()).getUsername())
                .reportStatus(EReportStatus.PENDING)
                .build();

        reportReelRepository.save(report);
        callAIServiceToCheck(reel.getCaption(), "REEL", report.getId());
        return "Success";
    }

    @Override
    @Transactional
    public String reportComment(Long id, String reason, String detail) {
        Long userId = securityUtil.getUserIdFromToken();
        if (reportCommentRepository.existsByUserIdAndCommentId(userId, id)) {
            throw new CustomException("You already reported this comment", HttpStatus.BAD_REQUEST);
        }

        var commentResponse = interactionGrpcClient.getCommentById(id);
        if (commentResponse.getCommentId() == 0) {
            throw new CustomException("Comment does not exist", HttpStatus.NOT_FOUND);
        }

        Long ownerId = commentResponse.getUserId();
        validateReportPermission(userId, ownerId);

        var userMap = getUserMap(List.of(ownerId, userId));

        ReportCommentModel report = ReportCommentModel.builder()
                .userId(userId)
                .commentId(commentResponse.getCommentId())
                .reason(reason)
                .detail(detail)
                .ownerId(ownerId)
                .content(commentResponse.getContent())
                .reporterName(userMap.get(userId).getUsername())
                .ownerCommentName(userMap.get(ownerId).getUsername())
                .reportStatus(EReportStatus.PENDING)
                .build();

        reportCommentRepository.save(report);
        callAIServiceToCheck(commentResponse.getContent(), "COMMENT", report.getId());
        return "Success";
    }


    @Override
    @Transactional
    public String handleReportPost(Long id, EReportStatus decision, String note) {
        ReportPostModel report = reportPostRepository.findById(id)
                .orElseThrow(() -> new CustomException("Report not found", HttpStatus.NOT_FOUND));

        if (report.getReportStatus() != EReportStatus.PENDING && report.getReportStatus() != EReportStatus.IN_REVIEW) {
            throw new CustomException("Report already processed", HttpStatus.BAD_REQUEST);
        }

        if (decision == EReportStatus.APPROVED) {
            PostModel post = report.getPostModel();
            post.setIsActive(false);
            postRepository.save(post);
        }

        report.setReportStatus(decision);
        report.setNote(note);
        reportPostRepository.save(report);
        return "Report " + decision.name();
    }

    @Override
    @Transactional
    public String handleReportReel(Long id, EReportStatus decision, String note) {
        ReportReelModel report = reportReelRepository.findById(id)
                .orElseThrow(() -> new CustomException("Report not found", HttpStatus.NOT_FOUND));

        if (report.getReportStatus() != EReportStatus.PENDING && report.getReportStatus() != EReportStatus.IN_REVIEW) {
            throw new CustomException("Report already processed", HttpStatus.BAD_REQUEST);
        }

        if (decision == EReportStatus.APPROVED) {
            ReelModel reel = report.getReelModel();
            reel.setIsActive(false);
            reelRepository.save(reel);
        }

        report.setReportStatus(decision);
        report.setNote(note);
        reportReelRepository.save(report);
        return "Report " + decision.name();
    }

    @Override
    @Transactional
    public String handleReportComment(Long id, EReportStatus decision, String note) {
        ReportCommentModel report = reportCommentRepository.findById(id)
                .orElseThrow(() -> new CustomException("Report not found", HttpStatus.NOT_FOUND));

        if (report.getReportStatus() != EReportStatus.PENDING && report.getReportStatus() != EReportStatus.IN_REVIEW) {
            throw new CustomException("Report already processed", HttpStatus.BAD_REQUEST);
        }

        if (decision == EReportStatus.APPROVED) {
            interactionGrpcClient.deleteCommentById(report.getCommentId());
            report.setCommentId(0L); // Đánh dấu comment đã bị xóa
        }

        report.setReportStatus(decision);
        report.setNote(note);
        reportCommentRepository.save(report);
        return "Report " + decision.name();
    }


    @Override
    public ReportInfoDTO searchReportPosts(int pageNo, int pageSize, EReportStatus status, String keyword) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        String statusStr = (status == null) ? "ALL" : status.name();

        var counts = reportPostRepository.getReportQuantitySummary();
        Page<ReportSummaryProjection> reportPage = reportPostRepository.searchReportPostsNative(statusStr, keyword, pageable);

        return new ReportInfoDTO(counts.getPendingCount(), counts.getApprovedCount(),
                counts.getInReviewCount(), counts.getRejectedCount(), reportPage);
    }

    @Override
    public ReportInfoDTO searchReportReels(int pageNo, int pageSize, EReportStatus status, String keyword) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        String statusStr = (status == null) ? "ALL" : status.name();

        var counts = reportReelRepository.getReportQuantitySummary();
        Page<ReportSummaryProjection> reportPage = reportReelRepository.searchReportsReelsNative(statusStr, keyword, pageable);

        return new ReportInfoDTO(counts.getPendingCount(), counts.getApprovedCount(),
                counts.getInReviewCount(), counts.getRejectedCount(), reportPage);
    }

    @Override
    public ReportInfoDTO searchReportComments(int pageNo, int pageSize, EReportStatus status, String keyword) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        String statusStr = (status == null) ? "ALL" : status.name();

        var counts = reportCommentRepository.getReportQuantitySummary();
        Page<ReportSummaryProjection> reportPage = reportCommentRepository.searchReportCommentsNative(statusStr, keyword, pageable);

        return new ReportInfoDTO(counts.getPendingCount(), counts.getApprovedCount(),
                counts.getInReviewCount(), counts.getRejectedCount(), reportPage);
    }


    @Override
    public ReportResponseDTO getPostReportById(Long id) {
        ReportPostModel model = reportPostRepository.findById(id)
                .orElseThrow(() -> new CustomException("Report not found", HttpStatus.NOT_FOUND));

        var userMap = getUserMap(List.of(model.getUserId(), model.getPostModel().getUserId()));

        return ReportResponseDTO.builder()
                .id(model.getId())
                .postId(model.getPostModel().getId())
                .userId(model.getUserId())
                .reason(model.getReason())
                .detail(model.getDetail())
                .reportStatus(model.getReportStatus().name())
                .createdAt(model.getCreatedAt())
                .reporterName(model.getReporterName())
                .ownerPostName(model.getOwnerPostName())
                .reporterAvatarUrl(userMap.get(model.getUserId()).getAvatar())
                .ownerPostAvatarUrl(userMap.get(model.getPostModel().getUserId()).getAvatar())
                .mediaUrls(model.getPostModel().getPostMediaModels().stream().map(PostMediaModel::getMediaUrl).toList())
                .caption(model.getPostModel().getCaption())
                .isActive(model.getPostModel().getIsActive())
                .note(model.getNote())
                .build();
    }

    @Override
    public ReportResponseDTO getReelReportById(Long id) {
        ReportReelModel model = reportReelRepository.findById(id)
                .orElseThrow(() -> new CustomException("Report not found", HttpStatus.NOT_FOUND));

        var userMap = getUserMap(List.of(model.getUserId(), model.getReelModel().getUserId()));

        return ReportResponseDTO.builder()
                .id(model.getId())
                .postId(model.getReelModel().getId())
                .userId(model.getUserId())
                .reason(model.getReason())
                .detail(model.getDetail())
                .reportStatus(model.getReportStatus().name())
                .createdAt(model.getCreatedAt())
                .reporterName(model.getReporterName())
                .ownerPostName(model.getOwnerPostName())
                .reporterAvatarUrl(userMap.get(model.getUserId()).getAvatar())
                .ownerPostAvatarUrl(userMap.get(model.getReelModel().getUserId()).getAvatar())
                .mediaUrls(List.of(model.getReelModel().getVideoUrl()))
                .caption(model.getReelModel().getCaption())
                .isActive(model.getReelModel().getIsActive())
                .predictAI(model.getPredictAI())
                .confidence(model.getConfidence())
                .note(model.getNote())
                .build();
    }

    @Override
    public ReportResponseDTO getCommentReportById(Long id) {
        ReportCommentModel model = reportCommentRepository.findById(id)
                .orElseThrow(() -> new CustomException("Report not found", HttpStatus.NOT_FOUND));

        var userMap = getUserMap(List.of(model.getUserId(), model.getOwnerId()));

        return ReportResponseDTO.builder()
                .id(model.getId())
                .postId(model.getCommentId())
                .userId(model.getUserId())
                .reason(model.getReason())
                .detail(model.getDetail())
                .reportStatus(model.getReportStatus().name())
                .createdAt(model.getCreatedAt())
                .reporterName(model.getReporterName())
                .ownerPostName(model.getOwnerCommentName())
                .reporterAvatarUrl(userMap.get(model.getUserId()).getAvatar())
                .ownerPostAvatarUrl(userMap.get(model.getOwnerId()).getAvatar())
                .content(model.getContent())
                .predictAI(model.getPredictAI())
                .confidence(model.getConfidence())
                .note(model.getNote())
                .build();
    }

    @Async
    public void callAIServiceToCheck(String content, String type, Long reportId) {
        try {
            Moderation.PredictionResponse prediction = aiModerationClient.checkText(content);
            double conf = (double) prediction.getConfidence();
            String label = prediction.getLabel();

            switch (type) {
                case "POST" -> reportPostRepository.findById(reportId).ifPresent(r -> {
                    r.setPredictAI(label);
                    r.setConfidence(conf);
                    reportPostRepository.save(r);
                });
                case "REEL" -> reportReelRepository.findById(reportId).ifPresent(r -> {
                    r.setPredictAI(label);
                    r.setConfidence(conf);
                    reportReelRepository.save(r);
                });
                case "COMMENT" -> reportCommentRepository.findById(reportId).ifPresent(r -> {
                    r.setPredictAI(label);
                    r.setConfidence(conf);
                    reportCommentRepository.save(r);
                });
            }
        } catch (Exception e) {
            log.error("AI Moderation failed for {} ID: {}", type, reportId, e);
        }
    }
}