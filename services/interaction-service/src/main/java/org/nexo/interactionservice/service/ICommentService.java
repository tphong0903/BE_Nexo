package org.nexo.interactionservice.service;

import org.nexo.interactionservice.dto.request.CommentDto;
import org.nexo.interactionservice.dto.response.ListCommentResponse;

public interface ICommentService {
    String saveComment(CommentDto dto);

    String deleteComment(Long id);

    ListCommentResponse getCommentOfPost(Long id, int pageNo, int pageSize);

    ListCommentResponse getCommentOfReel(Long id, int pageNo, int pageSize);

    ListCommentResponse getReplies(Long commentId, int pageNo, int pageSize);
}
