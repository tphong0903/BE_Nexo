package org.nexo.interactionservice.dto.request;

import lombok.*;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeCommentRequestDto {
    private Long id;
    private Long commentId;
    private Long userId;
}
