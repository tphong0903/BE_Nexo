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
    private Long postId;
    private Long userId;
    private String userName;
    private String avatarUrl;
    private String caption;
    private String visibility;
    private String tag;
    private Boolean isLike;
    private String mediaUrl;
    private Long quantityLike;
    private Long quantityComment;
    private List<UserTagDTO> listUserTag;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
