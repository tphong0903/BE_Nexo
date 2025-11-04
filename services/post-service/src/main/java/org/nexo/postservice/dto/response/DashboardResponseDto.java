package org.nexo.postservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardResponseDto {
    Long totalUser;
    Long totalPost;
    Long totalInteract;
    Long quantityReport;
    Double percentUser;
    Double percentPost;
    Double percentInteract;
}
