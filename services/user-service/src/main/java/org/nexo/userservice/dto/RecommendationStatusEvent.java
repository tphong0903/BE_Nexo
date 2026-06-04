package org.nexo.userservice.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendationStatusEvent {
    private String eventType;
    private Long userId;
    private Long targetUserId;  // dùng cho USER_BLOCKED / USER_UNBLOCKED
    private Instant timestamp;
}
