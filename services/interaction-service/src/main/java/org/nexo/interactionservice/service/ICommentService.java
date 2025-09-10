package org.nexo.interactionservice.service;

import org.nexo.interactionservice.dto.request.CommentDto;

public interface ICommentService {
    String saveComment(CommentDto dto);

    String deleteComment(Long id);


}
