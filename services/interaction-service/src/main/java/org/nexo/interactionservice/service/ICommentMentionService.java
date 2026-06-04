package org.nexo.interactionservice.service;

import org.nexo.interactionservice.model.CommentModel;

import java.util.List;

public interface ICommentMentionService {
    String addMentionComment(Long userId, CommentModel model);

    String syncMentionComment(List<Long> mentionedUserIds, CommentModel commentModel);
}
