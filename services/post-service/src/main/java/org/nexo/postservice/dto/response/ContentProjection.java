package org.nexo.postservice.dto.response;

import java.time.LocalDateTime;


import java.time.LocalDateTime;
import java.util.UUID;


public interface ContentProjection {

    Long getId();

    UUID getUuid();

    Long getUserId();

    String getType();

    String getAuthorName();

    String getCaption();

    Integer getLikeQuantity();

    Integer getCommentQuantity();

    String getVisibility();

    Boolean getIsActive();

    LocalDateTime getCreatedAt();

}