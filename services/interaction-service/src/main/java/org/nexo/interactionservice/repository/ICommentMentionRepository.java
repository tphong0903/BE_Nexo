package org.nexo.interactionservice.repository;

import org.nexo.interactionservice.model.CommentMentionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICommentMentionRepository extends JpaRepository<CommentMentionModel, Long> {
}
