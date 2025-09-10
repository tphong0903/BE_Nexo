package org.nexo.interactionservice.dto.request;

import lombok.*;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikePostRequestDto {
    private Long Id;
    private Long postId;
    private Long reelId;
    private Long userId;
}
