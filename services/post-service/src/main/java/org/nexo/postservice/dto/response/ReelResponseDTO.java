package org.nexo.postservice.dto.response;

import lombok.*;
import org.nexo.postservice.dto.UserTagDTO;

import java.time.LocalDateTime;
import java.util.List;

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
    private Boolean isLike;
    private String mediaUrl;
    private Long quantityLike;
    private Long quantityComment;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
