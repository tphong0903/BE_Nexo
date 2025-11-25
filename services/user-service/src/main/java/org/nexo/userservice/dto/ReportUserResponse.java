package org.nexo.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nexo.userservice.enums.EReportStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportUserResponse {

    private Long reporterId;
    private String reporterUsername;
    private Long reportedId;
    private String reportedUsername;
    private String reason;
    private EReportStatus status;
    private LocalDateTime createdAt;
}

