package org.nexo.postservice.dto.response;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ReelResponseDTO implements Serializable {
    private Long reelId;
    private Long userId;
    private String userName;
    private String avatarUrl;
    private String caption;
    private String visibility;
    private Boolean isLike;
    private String mediaUrl;
    private Long quantityLike;
    private Long quantityComment;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
