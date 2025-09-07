package org.nexo.interactionservice.repository;

import org.nexo.interactionservice.model.CommentModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICommentRepository extends JpaRepository<CommentModel, Long> {
}
