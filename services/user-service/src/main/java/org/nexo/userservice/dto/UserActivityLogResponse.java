package org.nexo.userservice.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityLogResponse {
    private Long id;
    private String action;
    private String detailsJson;
    private LocalDateTime createdAt;
}
