package org.nexo.userservice.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendationFollowedEvent {
    private String eventType;
    private Long followerId;
    private Long followingId;
    private Instant timestamp;
}
