package org.nexo.postservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest {
    private String reason;
    private String detail;
}