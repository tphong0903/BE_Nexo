package org.nexo.postservice.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityEvent {
    private String eventType;
    private Long userId;
    private Long targetId;
    private String targetType;
    private Instant occurredAt;
    private String metadata;
}
