package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nexo.messagingservice.enums.EReactionType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionDetailDTO {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private EReactionType reactionType;
    private LocalDateTime createdAt;
}
