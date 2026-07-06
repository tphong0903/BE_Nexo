package org.nexo.postservice.dto.response;

import java.time.LocalDateTime;

public interface ReportSummaryProjection {
    String getPredictAI();

    Double getConfidence();

    String getReason();

    String getReporterName();

    String getOwnerName();

    String getReportStatus();

    LocalDateTime getCreatedAt();

    Long getId();
}