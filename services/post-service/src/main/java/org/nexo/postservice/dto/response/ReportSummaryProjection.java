package org.nexo.postservice.dto.response;

import java.time.LocalDateTime;

public interface ReportSummaryProjection {
    String getReason();

    String getReporterName();

    String getOwnerPostName();

    String getOwnerCommentName();

    String getReportStatus();

    LocalDateTime getCreatedAt();

    Long getId();
}