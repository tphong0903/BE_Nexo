package org.nexo.postservice.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportInfoDTO {

    Long pendingQuantity;
    Long approvedQuantity;
    Long processingQuantity;
    Long rejectQuantity;
    Page<ReportSummaryProjection> reportSummaries;
}
