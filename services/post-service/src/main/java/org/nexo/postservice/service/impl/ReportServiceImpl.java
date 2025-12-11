package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.interaction.InteractionServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.dto.response.ReportInfoDTO;
import org.nexo.postservice.dto.response.ReportResponseDTO;
import org.nexo.postservice.dto.response.ReportSummaryProjection;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.*;
import org.nexo.postservice.repository.*;
import org.nexo.postservice.service.GrpcServiceImpl.client.InteractionGrpcClient;
import org.nexo.postservice.service.GrpcServiceImpl.client.UserGrpcClient;
import org.nexo.postservice.service.IReportService;
import org.nexo.postservice.util.Enum.EReportStatus;
import org.nexo.postservice.util.Enum.EVisibilityPost;
import org.nexo.postservice.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {
    private final IReportPostRepository reportPostRepository;
    private final IReportReelRepository reportReelRepository;
    private final IReportCommentRepository reportCommentRepository;
    private final IPostRepository postRepository;
    private final IReelRepository reelRepository;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;
    private final InteractionGrpcClient interactionGrpcClient;

    @Override
    public String reportPost(Long id, String reason, String detail) {
        Long userId = securityUtil.getUserIdFromToken();
        boolean exists = reportPostRepository.existsByUserIdAndPostModel_Id(userId, id);
        if (exists) {
            throw new CustomException("You reported this post", HttpStatus.BAD_REQUEST);
        }
        PostModel postModel = postRepository.findById(id).orElseThrow(() -> new CustomException("Post is not exist", HttpStatus.BAD_REQUEST));
        boolean isAllow = false;
        if (postModel.getUserId().equals(userId)) {
            isAllow = true;
        } else {
            UserServiceProto.CheckFollowResponse followResponse = userGrpcClient.checkFollow(userId, postModel.getUserId());
            if (!followResponse.getIsPrivate() || followResponse.getIsFollow()) {
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to report this Post", HttpStatus.BAD_REQUEST);

        List<UserServiceProto.UserDTOResponse2> users = userGrpcClient.getUsersByIds(List.of(postModel.getUserId(), userId));

        ReportPostModel report = ReportPostModel.builder()
                .userId(userId)
                .postModel(postModel)
                .reason(reason)
                .detail(detail)
                .reporterName(users.get(1).getUsername())
                .ownerPostName(users.get(0).getUsername())
                .reportStatus(EReportStatus.PENDING)
                .build();

        reportPostRepository.save(report);
        return "Success";
    }

    @Override
    public String reportReel(Long id, String reason, String detail) {
        Long userId = securityUtil.getUserIdFromToken();
        boolean exists = reportReelRepository.existsByUserIdAndReelModel_Id(userId, id);
        if (exists) {
            throw new CustomException("You reported this post", HttpStatus.BAD_REQUEST);
        }
        ReelModel reelModel = reelRepository.findById(id).orElseThrow(() -> new CustomException("Reel is not exist", HttpStatus.BAD_REQUEST));
        boolean isAllow = false;
        if (reelModel.getUserId().equals(userId)) {
            isAllow = true;
        } else {
            UserServiceProto.CheckFollowResponse followResponse = userGrpcClient.checkFollow(userId, reelModel.getUserId());
            if (!followResponse.getIsPrivate() || followResponse.getIsFollow()) {
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to report this Post", HttpStatus.BAD_REQUEST);

        List<UserServiceProto.UserDTOResponse2> users = userGrpcClient.getUsersByIds(List.of(reelModel.getUserId(), userId));

        ReportReelModel report = ReportReelModel.builder()
                .userId(userId)
                .reelModel(reelModel)
                .reason(reason)
                .detail(detail)
                .reporterName(users.get(1).getUsername())
                .ownerPostName(users.get(0).getUsername())
                .reportStatus(EReportStatus.PENDING)
                .build();

        reportReelRepository.save(report);
        return "Success";
    }

    @Override
    public String reportComment(Long id, String reason, String detail) {
        Long userId = securityUtil.getUserIdFromToken();
        boolean exists = reportCommentRepository.existsByUserIdAndCommentId(userId, id);
        if (exists) {
            throw new CustomException("You reported this post", HttpStatus.BAD_REQUEST);
        }

        InteractionServiceOuterClass.GetCommentByIdResponse commentModel = interactionGrpcClient.getCommentById(id);
        if (commentModel.getCommentId() == 0)
            throw new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST);
        AbstractPost abstractPost = null;

        if (commentModel.getPostId() != 0)
            abstractPost = postRepository.findById(commentModel.getPostId()).orElseThrow(() -> new CustomException("Post is not exist", HttpStatus.BAD_REQUEST));
        else
            abstractPost = reelRepository.findById(commentModel.getReelId()).orElseThrow(() -> new CustomException("Reel is not exist", HttpStatus.BAD_REQUEST));
        boolean isAllow = false;
        if (abstractPost.getUserId().equals(userId)) {
            isAllow = true;
        } else {
            UserServiceProto.CheckFollowResponse followResponse = userGrpcClient.checkFollow(userId, abstractPost.getUserId());
            if (!followResponse.getIsPrivate() || followResponse.getIsFollow()) {
                isAllow = true;
            }
        }
        if (!isAllow)
            throw new CustomException("Dont allow to report this Post", HttpStatus.BAD_REQUEST);

        List<UserServiceProto.UserDTOResponse2> users = userGrpcClient.getUsersByIds(List.of(commentModel.getUserId(), userId));

        ReportCommentModel report = ReportCommentModel.builder()
                .userId(userId)
                .commentId(commentModel.getCommentId())
                .reason(reason)
                .detail(detail)
                .ownerId(commentModel.getUserId())
                .content(commentModel.getContent())
                .reporterName(users.get(1).getUsername())
                .ownerCommentName(users.get(0).getUsername())
                .reportStatus(EReportStatus.PENDING)
                .build();

        reportCommentRepository.save(report);
        return "Success";
    }

    @Override
    public String handleReportPost(Long id, EReportStatus decision, String note) {

        ReportPostModel report = reportPostRepository.findById(id)
                .orElseThrow(() -> new CustomException("Report not found", HttpStatus.BAD_REQUEST));

        if (report.getReportStatus() != EReportStatus.PENDING && report.getReportStatus() != EReportStatus.IN_REVIEW) {
            throw new CustomException("This report has already been processed", HttpStatus.BAD_REQUEST);
        }

        PostModel post = report.getPostModel();
        if (post == null) {
            throw new CustomException("Post not found for this report", HttpStatus.BAD_REQUEST);
        }

        switch (decision) {
            case APPROVED:
                post.setIsActive(false);
                postRepository.save(post);
                report.setReportStatus(EReportStatus.APPROVED);
                break;

            case REJECTED:
                report.setReportStatus(EReportStatus.REJECTED);
                break;

            case IN_REVIEW:
                report.setReportStatus(EReportStatus.IN_REVIEW);
                break;

            default:
                throw new CustomException("Invalid decision", HttpStatus.BAD_REQUEST);
        }

        report.setNote(note);
        reportPostRepository.save(report);

        return "Report processed successfully with decision: " + decision.name();
    }


    @Override
    public String handleReportReel(Long id, EReportStatus decision, String note) {
        ReportReelModel report = reportReelRepository.findById(id)
                .orElseThrow(() -> new CustomException("Report not found", HttpStatus.BAD_REQUEST));

        if (report.getReportStatus() != EReportStatus.PENDING && report.getReportStatus() != EReportStatus.IN_REVIEW) {
            throw new CustomException("This report has already been processed", HttpStatus.BAD_REQUEST);
        }

        ReelModel reel = report.getReelModel();
        if (reel == null) {
            throw new CustomException("Reel not found for this report", HttpStatus.BAD_REQUEST);
        }

        switch (decision) {
            case APPROVED:
                reel.setIsActive(false);
                reelRepository.save(reel);
                report.setReportStatus(EReportStatus.APPROVED);
                break;

            case REJECTED:
                report.setReportStatus(EReportStatus.REJECTED);
                break;

            case IN_REVIEW:
                report.setReportStatus(EReportStatus.IN_REVIEW);
                break;

            default:
                throw new CustomException("Invalid decision", HttpStatus.BAD_REQUEST);
        }
        report.setNote(note);
        reportReelRepository.save(report);
        return "Report processed successfully with decision: " + decision.name();
    }

    @Override
    public String handleReportComment(Long id, EReportStatus decision, String note) {
        ReportCommentModel report = reportCommentRepository.findById(id)
                .orElseThrow(() -> new CustomException("Report not found", HttpStatus.BAD_REQUEST));

        if (report.getReportStatus() != EReportStatus.PENDING && report.getReportStatus() != EReportStatus.IN_REVIEW) {
            throw new CustomException("This report has already been processed", HttpStatus.BAD_REQUEST);
        }

        switch (decision) {
            case APPROVED:
                interactionGrpcClient.deleteCommentById(id);
                report.setReportStatus(EReportStatus.APPROVED);
                report.setCommentId(0L);
                break;

            case REJECTED:
                report.setReportStatus(EReportStatus.REJECTED);
                break;

            case IN_REVIEW:
                report.setReportStatus(EReportStatus.IN_REVIEW);
                break;

            default:
                throw new CustomException("Invalid decision", HttpStatus.BAD_REQUEST);
        }
        report.setNote(note);
        reportCommentRepository.save(report);
        return "Report processed successfully with decision: " + decision.name();
    }

    @Override
    public ReportInfoDTO searchReportPosts(int pageNo, int pageSize, EReportStatus status, String keyword) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        if (status == null) {
            status = EReportStatus.ALL;
        }
        List<Object[]> countsList = reportPostRepository.getReportQuantitySummary();
        Object[] counts = countsList.get(0);

        Long pending = ((Number) counts[0]).longValue();
        Long inReview = ((Number) counts[1]).longValue();
        Long approved = ((Number) counts[2]).longValue();
        Long rejected = ((Number) counts[3]).longValue();

        Page<ReportSummaryProjection> reportPage = reportPostRepository.searchReportPostsNative(status.name(), keyword, pageable);
        return new ReportInfoDTO(pending, approved, inReview, rejected, reportPage);
    }

    @Override
    public ReportInfoDTO searchReportReels(int pageNo, int pageSize, EReportStatus status, String keyword) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        if (status == null) {
            status = EReportStatus.ALL;
        }
        List<Object[]> countsList = reportReelRepository.getReportQuantitySummary();
        Object[] counts = countsList.get(0);

        Long pending = ((Number) counts[0]).longValue();
        Long inReview = ((Number) counts[1]).longValue();
        Long approved = ((Number) counts[2]).longValue();
        Long rejected = ((Number) counts[3]).longValue();

        Page<ReportSummaryProjection> reportPage = reportReelRepository
                .searchReportsReelsNative(status.name(), keyword, pageable);
        return new ReportInfoDTO(pending, approved, inReview, rejected, reportPage);
    }

    @Override
    public ReportInfoDTO searchReportComments(int pageNo, int pageSize, EReportStatus status, String keyword) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        if (status == null) {
            status = EReportStatus.ALL;
        }
        List<Object[]> countsList = reportCommentRepository.getReportQuantitySummary();
        Object[] counts = countsList.get(0);

        Long pending = ((Number) counts[0]).longValue();
        Long inReview = ((Number) counts[1]).longValue();
        Long approved = ((Number) counts[2]).longValue();
        Long rejected = ((Number) counts[3]).longValue();

        Page<ReportSummaryProjection> reportPage = reportCommentRepository
                .searchReportCommentsNative(status.name(), keyword, pageable);
        return new ReportInfoDTO(pending, approved, inReview, rejected, reportPage);
    }

    @Override
    public ReportResponseDTO getPostReportById(Long id) {
        ReportPostModel model = reportPostRepository.findById(id).orElseThrow(() -> new CustomException("Report not found", HttpStatus.BAD_REQUEST));

        List<UserServiceProto.UserDTOResponse2> users = userGrpcClient.getUsersByIds(List.of(model.getUserId(), model.getPostModel().getUserId()));

        return ReportResponseDTO.builder()
                .postId(model.getPostModel().getId())
                .id(model.getId())
                .userId(model.getUserId())
                .reason(model.getReason())
                .detail(model.getDetail())
                .reportStatus(String.valueOf(model.getReportStatus()))
                .createdAt(model.getCreatedAt())
                .reporterName(model.getReporterName())
                .ownerPostName(model.getOwnerPostName())
                .reporterAvatarUrl(users.get(0).getAvatar())
                .ownerPostAvatarUrl(users.get(1).getAvatar())
                .mediaUrls(model.getPostModel().getPostMediaModels().stream().map(PostMediaModel::getMediaUrl).toList())
                .caption(model.getPostModel().getCaption())
                .note(model.getNote())
                .isActive(model.getPostModel().getIsActive())
                .build();
    }

    @Override
    public ReportResponseDTO getReelReportById(Long id) {
        ReportReelModel model = reportReelRepository.findById(id).orElseThrow(() -> new CustomException("Report not found", HttpStatus.BAD_REQUEST));

        List<UserServiceProto.UserDTOResponse2> users = userGrpcClient.getUsersByIds(List.of(model.getUserId(), model.getReelModel().getUserId()));

        return ReportResponseDTO.builder()
                .postId(model.getReelModel().getId())
                .id(model.getId())
                .userId(model.getUserId())
                .reason(model.getReason())
                .detail(model.getDetail())
                .reportStatus(String.valueOf(model.getReportStatus()))
                .createdAt(model.getCreatedAt())
                .reporterName(model.getReporterName())
                .ownerPostName(model.getOwnerPostName())
                .reporterAvatarUrl(users.get(0).getAvatar())
                .ownerPostAvatarUrl(users.get(1).getAvatar())
                .mediaUrls(List.of(model.getReelModel().getVideoUrl()))
                .caption(model.getReelModel().getCaption())
                .note(model.getNote())
                .isActive(model.getReelModel().getIsActive())
                .build();
    }

    @Override
    public ReportResponseDTO getCommentReportById(Long id) {
        ReportCommentModel model = reportCommentRepository.findById(id).orElseThrow(() -> new CustomException("Report not found", HttpStatus.BAD_REQUEST));

        List<UserServiceProto.UserDTOResponse2> users = userGrpcClient.getUsersByIds(List.of(model.getUserId(), model.getOwnerId()));

        return ReportResponseDTO.builder()
                .postId(model.getCommentId())
                .id(model.getId())
                .userId(model.getUserId())
                .reason(model.getReason())
                .detail(model.getDetail())
                .reportStatus(String.valueOf(model.getReportStatus()))
                .createdAt(model.getCreatedAt())
                .reporterName(model.getReporterName())
                .ownerPostName(model.getOwnerCommentName())
                .reporterAvatarUrl(users.get(0).getAvatar())
                .ownerPostAvatarUrl(users.get(1).getAvatar())
                .note(model.getNote())
                .content(model.getContent())
                .build();
    }
}
