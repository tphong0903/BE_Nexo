package org.nexo.interactionservice.repository;

import org.nexo.interactionservice.model.LikeCommentModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ILikeCommentRepository extends JpaRepository<LikeCommentModel, Long> {
}
