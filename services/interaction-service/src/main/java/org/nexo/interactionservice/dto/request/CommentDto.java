package org.nexo.interactionservice.dto.request;

import lombok.*;

import java.util.List;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private Long userId;
    private Long postId;
    private Long reelId;
    private Long parentId;
    private String content;
    private List<Long> listMentionUserId;
}
