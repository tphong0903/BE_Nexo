package org.nexo.userservice.dto;

import java.time.LocalDateTime;

import org.nexo.userservice.enums.EStatusFollow;

public interface RecommendationFollowExportDTO {
    Long getFollowerId();

    Long getFollowingId();

    EStatusFollow getStatus();

    Boolean getIsCloseFriend();

    LocalDateTime getCreatedAt();
}
