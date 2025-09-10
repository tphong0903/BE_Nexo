package org.nexo.interactionservice.service;

import org.nexo.interactionservice.model.CommentModel;

public interface ICommentMentionService {
    String addMentionComment(Long userId, CommentModel model);
}
