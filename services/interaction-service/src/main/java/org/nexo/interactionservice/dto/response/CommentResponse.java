package org.nexo.interactionservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String avatarUrl;
    private String content;
    private Long parentId;
    private Long quantityLike;
    private List<CommentResponse> responseChildList;
    private LocalDateTime createdAt;
    private boolean hasMoreReplies;
    private boolean isLike;
}
