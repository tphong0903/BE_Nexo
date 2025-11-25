package org.nexo.userservice.service;

import org.nexo.userservice.dto.CreateReportRequest;
import org.nexo.userservice.dto.PageModelResponse;
import org.nexo.userservice.dto.ReportUserResponse;
import org.nexo.userservice.dto.UpdateReportStatusRequest;
import org.nexo.userservice.enums.EReportStatus;
import org.springframework.data.domain.Pageable;

public interface ReportUserService {

    ReportUserResponse createReport(String token, String username, CreateReportRequest request);

    PageModelResponse<ReportUserResponse> getReportsByReporter(String token, Pageable pageable, String search);

    PageModelResponse<ReportUserResponse> getReportsByReported(String token, String username, Pageable pageable,
            String search);

    PageModelResponse<ReportUserResponse> getAllReports(EReportStatus status, Pageable pageable);

    ReportUserResponse updateReportStatus(Long reporterId, Long reportedId, UpdateReportStatusRequest request);

    void deleteReport(String token, String username);
}
