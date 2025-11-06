package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.model.PostModel;
import org.nexo.postservice.model.ReelModel;
import org.nexo.postservice.model.ReportPostModel;
import org.nexo.postservice.model.ReportReelModel;
import org.nexo.postservice.repository.IPostRepository;
import org.nexo.postservice.repository.IReelRepository;
import org.nexo.postservice.repository.IReportPostRepository;
import org.nexo.postservice.repository.IReportReelRepository;
import org.nexo.postservice.service.GrpcServiceImpl.client.UserGrpcClient;
import org.nexo.postservice.service.IReportService;
import org.nexo.postservice.util.Enum.EReportStatus;
import org.nexo.postservice.util.Enum.EVisibilityPost;
import org.nexo.postservice.util.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {
    private final IReportPostRepository reportPostRepository;
    private final IReportReelRepository reportReelRepository;
    private final IPostRepository postRepository;
    private final IReelRepository reelRepository;
    private final SecurityUtil securityUtil;
    private final UserGrpcClient userGrpcClient;

    @Override
    public String reportPost(Long id, String reason) {
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

        ReportPostModel report = ReportPostModel.builder()
                .userId(userId)
                .postModel(postModel)
                .reason(reason)
                .reportStatus(EReportStatus.PENDING)
                .build();

        reportPostRepository.save(report);
        return "Success";
    }

    @Override
    public String reportReel(Long id, String reason) {
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

        ReportReelModel report = ReportReelModel.builder()
                .userId(userId)
                .reelModel(reelModel)
                .reason(reason)
                .reportStatus(EReportStatus.PENDING)
                .build();

        reportReelRepository.save(report);
        return "Success";
    }

    @Override
    public String handleReportPost(Long id, EReportStatus decision) {
        Long adminId = securityUtil.getUserIdFromToken();

        ReportPostModel report = reportPostRepository.findById(id)
                .orElseThrow(() -> new CustomException("Report not found", HttpStatus.BAD_REQUEST));

        if (report.getReportStatus() != EReportStatus.PENDING) {
            throw new CustomException("This report has already been processed", HttpStatus.BAD_REQUEST);
        }

        PostModel post = report.getPostModel();
        if (post == null) {
            throw new CustomException("Post not found for this report", HttpStatus.BAD_REQUEST);
        }

        // 4️⃣ Xử lý theo quyết định của admin
        switch (decision) {
            case APPROVED:
                post.setIsActive(false);
                postRepository.save(post);
                report.setReportStatus(EReportStatus.APPROVED);
                break;

            case REJECTED:
                report.setReportStatus(EReportStatus.REJECTED);
                break;

            case CLOSED:
                report.setReportStatus(EReportStatus.CLOSED);
                break;

            default:
                throw new CustomException("Invalid decision", HttpStatus.BAD_REQUEST);
        }

        reportPostRepository.save(report);

        return "Report processed successfully with decision: " + decision.name();
    }


    @Override
    public String handleReportReel(Long id, EReportStatus decision) {
        Long adminId = securityUtil.getUserIdFromToken();

        ReportReelModel report = reportReelRepository.findById(id)
                .orElseThrow(() -> new CustomException("Report not found", HttpStatus.BAD_REQUEST));

        if (report.getReportStatus() != EReportStatus.PENDING) {
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

            case CLOSED:
                report.setReportStatus(EReportStatus.CLOSED);
                break;

            default:
                throw new CustomException("Invalid decision", HttpStatus.BAD_REQUEST);
        }


        reportReelRepository.save(report);

        return "Report processed successfully with decision: " + decision.name();
    }

    @Override
    public Page<ReportPostModel> getAllReportPosts(int pageNo, int pageSize, EReportStatus status) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());

        if (status != null) {
            return reportPostRepository.findByReportStatus(status, pageable);
        }

        return reportPostRepository.findAll(pageable);
    }

    @Override
    public Page<ReportReelModel> getAllReportReels(int pageNo, int pageSize, EReportStatus status) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());

        if (status != null) {
            return reportReelRepository.findByReportStatus(status, pageable);
        }
        return reportReelRepository.findAll(pageable);
    }

    @Override
    public Page<ReportPostModel> searchReportPosts(int pageNo, int pageSize, EReportStatus status, String keyword) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        return reportPostRepository.searchReports(status, keyword, pageable);
    }

    @Override
    public Page<ReportReelModel> searchReportReels(int pageNo, int pageSize, EReportStatus status, String keyword) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("id").descending());
        return reportReelRepository.searchReports(status, keyword, pageable);
    }
}
