package org.nexo.interactionservice.dto.response;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListCommentResponse {
    private Long postId;
    private List<CommentResponse> commentResponseList;


    private int pageNo;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
