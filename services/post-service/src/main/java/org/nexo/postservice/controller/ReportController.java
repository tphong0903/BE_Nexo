package org.nexo.postservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.postservice.dto.ReportRequest;
import org.nexo.postservice.dto.response.ResponseData;
import org.nexo.postservice.model.ReportPostModel;
import org.nexo.postservice.model.ReportReelModel;
import org.nexo.postservice.service.IReportService;
import org.nexo.postservice.util.Enum.EReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
                reportService.reportPost(id, request.getReason())
        );
    }

    @PostMapping("/reel/{id}")
    public ResponseData<String> reportReel(
            @PathVariable Long id,
            @RequestBody ReportRequest request) {
        return new ResponseData<>(
                HttpStatus.CREATED.value(),
                "Success",
                reportService.reportReel(id, request.getReason())
        );
    }


    @GetMapping("/posts")
    public ResponseEntity<Page<ReportPostModel>> getAllPostReports(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) EReportStatus status,
            @RequestParam(required = false) String keyword
    ) {
        Page<ReportPostModel> reports = reportService.searchReportPosts(pageNo, pageSize, status, keyword);
        return ResponseEntity.ok(reports);
    }


    @GetMapping("/reels")
    public ResponseEntity<Page<ReportReelModel>> getAllReelReports(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) EReportStatus status,
            @RequestParam(required = false) String keyword
    ) {
        Page<ReportReelModel> reports = reportService.searchReportReels(pageNo, pageSize, status, keyword);
        return ResponseEntity.ok(reports);
    }
}
