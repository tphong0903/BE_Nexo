package org.nexo.interactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedPostResponseDTO implements Serializable {
    private Long savedPostId;
    private Long postId;
    private Long ownerId;
    private String ownerUsername;
    private String ownerAvatarUrl;
    private String caption;
    private String visibility;
    private List<String> mediaUrls;
    private Long quantityLike;
    private Long quantityComment;
    private Boolean isLike;
    private LocalDateTime postCreatedAt;
    private LocalDateTime savedAt;
}
