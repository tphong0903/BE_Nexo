package org.nexo.userservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nexo.userservice.enums.EReportStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReportStatusRequest {

    @NotNull(message = "Status is required")
    private EReportStatus status;
}

