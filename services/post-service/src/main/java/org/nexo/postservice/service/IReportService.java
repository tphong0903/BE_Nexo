package org.nexo.postservice.service;

import org.nexo.postservice.dto.response.ReportInfoDTO;
import org.nexo.postservice.dto.response.ReportResponseDTO;
import org.nexo.postservice.dto.response.ReportSummaryProjection;
import org.nexo.postservice.model.ReportPostModel;
import org.nexo.postservice.model.ReportReelModel;
import org.nexo.postservice.util.Enum.EReportStatus;
import org.springframework.data.domain.Page;

public interface IReportService {
    String reportPost(Long id, String reason, String detail);

    String reportReel(Long id, String reason, String detail);

    String reportComment(Long id, String reason, String detail);

    String handleReportPost(Long id, EReportStatus decision, String note);

    String handleReportReel(Long id, EReportStatus decision, String note);

    String handleReportComment(Long id, EReportStatus decision, String note);

    ReportInfoDTO searchReportPosts(int pageNo, int pageSize, EReportStatus status, String keyword);

    ReportInfoDTO searchReportReels(int pageNo, int pageSize, EReportStatus status, String keyword);

    ReportInfoDTO searchReportComments(int pageNo, int pageSize, EReportStatus status, String keyword);

    ReportResponseDTO getPostReportById(Long id);

    ReportResponseDTO getReelReportById(Long id);

    ReportResponseDTO getCommentReportById(Long id);
}
