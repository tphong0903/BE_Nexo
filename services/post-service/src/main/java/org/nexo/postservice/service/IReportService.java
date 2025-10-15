package org.nexo.postservice.service;

import org.nexo.postservice.model.ReportPostModel;
import org.nexo.postservice.model.ReportReelModel;
import org.nexo.postservice.util.Enum.EReportStatus;
import org.springframework.data.domain.Page;

public interface IReportService {
    String reportPost(Long id, String reason);

    String reportReel(Long id, String reason);

    String handleReportPost(Long id, EReportStatus decision);

    String handleReportReel(Long id, EReportStatus decision);

    Page<ReportPostModel> getAllReportPosts(int pageNo, int pageSize, EReportStatus status);

    Page<ReportReelModel> getAllReportReels(int pageNo, int pageSize, EReportStatus status);

    Page<ReportPostModel> searchReportPosts(int pageNo, int pageSize, EReportStatus status, String keyword);

    Page<ReportReelModel> searchReportReels(int pageNo, int pageSize, EReportStatus status, String keyword);
}
