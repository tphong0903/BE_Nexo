package org.nexo.feedservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import org.nexo.feedservice.dto.UserTagDTO;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDTO {
    private Long postId;
    private Long userId;
    private String userName;
    private String avatarUrl;
    private String caption;
    private String visibility;
    private String tag;
    private List<String> mediaUrl;
    private Long quantityLike;
    private Long quantityComment;
    private List<UserTagDTO> listUserTag;
    private Boolean isActive;
    private Boolean isLike;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
