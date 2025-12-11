package org.nexo.postservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.postservice.dto.ReportRequest;
import org.nexo.postservice.dto.response.ReportSummaryProjection;
import org.nexo.postservice.dto.response.ResponseData;
import org.nexo.postservice.model.ReportPostModel;
import org.nexo.postservice.model.ReportReelModel;
import org.nexo.postservice.service.IReportService;
import org.nexo.postservice.util.Enum.EReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
@Validated
@Slf4j
@RequiredArgsConstructor
public class ReportController {
    private final IReportService reportService;

    @PostMapping("/post/{id}")
    public ResponseData<String> reportPost(
            @PathVariable Long id,
            @RequestBody ReportRequest request) {
        return new ResponseData<>(
                HttpStatus.CREATED.value(),
                "Success",
                reportService.reportPost(id, request.getReason(), request.getDetail())
        );
    }

    @PostMapping("/reel/{id}")
    public ResponseData<String> reportReel(
            @PathVariable Long id,
            @RequestBody ReportRequest request) {
        return new ResponseData<>(
                HttpStatus.CREATED.value(),
                "Success",
                reportService.reportReel(id, request.getReason(), request.getDetail())
        );
    }

    @PostMapping("/comment/{id}")
    public ResponseData<String> reportComment(
            @PathVariable Long id,
            @RequestBody ReportRequest request) {
        return new ResponseData<>(
                HttpStatus.CREATED.value(),
                "Success",
                reportService.reportComment(id, request.getReason(), request.getDetail())
        );
    }

    @GetMapping("/posts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseData<?> getAllPostReports(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) EReportStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", reportService.searchReportPosts(pageNo, pageSize, status, keyword));
    }


    @GetMapping("/reels")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseData<?> getAllReelReports(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) EReportStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", reportService.searchReportReels(pageNo, pageSize, status, keyword));
    }

    @GetMapping("/comments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseData<?> getAllCommentReports(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) EReportStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", reportService.searchReportComments(pageNo, pageSize, status, keyword));
    }


    @GetMapping("/posts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseData<?> getReportPostById(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", reportService.getPostReportById(id));
    }

    @GetMapping("/reels/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseData<?> getReportReelById(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", reportService.getReelReportById(id));
    }

    @GetMapping("/comments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseData<?> getReportCommentById(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", reportService.getCommentReportById(id));
    }

    @PutMapping("/post/{id}/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseData<String> handleReportPost(
            @PathVariable Long id,
            @PathVariable String status,
            @RequestParam String note
    ) {

        if (!status.equals("IN_REVIEW") && !status.equals("APPROVED") && !status.equals("REJECTED")) {
            return new ResponseData<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid status value",
                    null
            );
        }
        return new ResponseData<>(
                HttpStatus.CREATED.value(),
                "Success",
                reportService.handleReportPost(id, EReportStatus.valueOf(status), note)
        );
    }

    @PutMapping("/reel/{id}/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseData<String> handleReportReel(
            @PathVariable Long id,
            @PathVariable String status,
            @RequestParam String note
    ) {

        if (!status.equals("IN_REVIEW") && !status.equals("APPROVED") && !status.equals("REJECTED")) {
            return new ResponseData<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid status value",
                    null
            );
        }
        return new ResponseData<>(
                HttpStatus.CREATED.value(),
                "Success",
                reportService.handleReportReel(id, EReportStatus.valueOf(status), note)
        );
    }

    @PutMapping("/comment/{id}/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseData<String> handleReportComment(
            @PathVariable Long id,
            @PathVariable String status,
            @RequestParam String note
    ) {

        if (!status.equals("IN_REVIEW") && !status.equals("APPROVED") && !status.equals("REJECTED")) {
            return new ResponseData<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid status value",
                    null
            );
        }
        return new ResponseData<>(
                HttpStatus.CREATED.value(),
                "Success",
                reportService.handleReportComment(id, EReportStatus.valueOf(status), note)
        );
    }
}
