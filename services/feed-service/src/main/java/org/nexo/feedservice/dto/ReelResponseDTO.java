package org.nexo.feedservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReelResponseDTO {
    private Long reelId;
    private Long userId;
    private String userName;
    private String avatarUrl;
    private String caption;
    private String visibility;
    private String mediaUrl;
    private Long quantityLike;
    private Long quantityComment;
    private Boolean isActive;
    private Boolean isLike;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
