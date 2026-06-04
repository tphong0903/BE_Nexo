package org.nexo.userservice.dto;

import java.time.LocalDateTime;

import org.nexo.userservice.enums.EAccountStatus;

public interface RecommendationUserExportDTO {
    Long getId();

    String getUsername();

    String getFullName();

    String getBio();

    Boolean getIsPrivate();

    Boolean getOnlineStatus();

    EAccountStatus getAccountStatus();

    LocalDateTime getCreatedAt();

    LocalDateTime getLastActiveAt();

    Double getActivityScore();

    Double getPostFrequency();

    Double getMutualInteractions();
}
