package org.nexo.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.userservice.dto.CreateReportRequest;
import org.nexo.userservice.dto.PageModelResponse;
import org.nexo.userservice.dto.ReportUserResponse;
import org.nexo.userservice.dto.ResponseData;
import org.nexo.userservice.dto.UpdateReportStatusRequest;
import org.nexo.userservice.enums.EReportStatus;
import org.nexo.userservice.service.ReportUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportUserController {

    private final ReportUserService reportUserService;

    @PostMapping("/{username}")
    public ResponseData<?> createReport(
            @PathVariable String username,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @Valid @RequestBody CreateReportRequest request) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        ReportUserResponse response = reportUserService.createReport(accessToken, username, request);
        return ResponseData.builder()
                .status(200)
                .message("User reported successfully")
                .data(response)
                .build();
    }

    @GetMapping("/my-reports")
    public ResponseData<?> getMyReports(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        PageModelResponse<ReportUserResponse> reports = reportUserService.getReportsByReporter(accessToken, pageable,
                search);
        return ResponseData.builder()
                .status(200)
                .message("Reports retrieved successfully")
                .data(reports)
                .build();
    }

    @GetMapping("/reported/{username}")
    public ResponseData<?> getReportsByReported(
            @PathVariable String username,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        PageModelResponse<ReportUserResponse> reports = reportUserService.getReportsByReported(accessToken, username,
                pageable, search);
        return ResponseData.builder()
                .status(200)
                .message("Reports retrieved successfully")
                .data(reports)
                .build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseData<?> getAllReports(
            @RequestParam(value = "status", required = false) EReportStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        PageModelResponse<ReportUserResponse> reports = reportUserService.getAllReports(status, pageable);
        return ResponseData.builder()
                .status(200)
                .message("All reports retrieved successfully")
                .data(reports)
                .build();
    }

    @PutMapping("/{reporterId}/{reportedId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseData<?> updateReportStatus(
            @PathVariable Long reporterId,
            @PathVariable Long reportedId,
            @Valid @RequestBody UpdateReportStatusRequest request) {
        ReportUserResponse response = reportUserService.updateReportStatus(reporterId, reportedId, request);
        return ResponseData.builder()
                .status(200)
                .message("Report status updated successfully")
                .data(response)
                .build();
    }

    @DeleteMapping("/{username}")
    public ResponseData<?> deleteReport(
            @PathVariable String username,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        reportUserService.deleteReport(accessToken, username);
        return ResponseData.builder()
                .status(200)
                .message("Report deleted successfully")
                .data(null)
                .build();
    }
}
