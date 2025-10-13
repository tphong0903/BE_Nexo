package org.nexo.feedservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessagePostDTO {
    private Long postId;
    private Long authorId;
    private Long createdAt;
}
