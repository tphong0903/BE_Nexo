package org.nexo.postservice.dto;

public interface ReportCountProjection {
    Long getPendingCount();

    Long getInReviewCount();

    Long getApprovedCount();

    Long getRejectedCount();
}