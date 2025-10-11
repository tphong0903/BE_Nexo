package org.nexo.interactionservice.repository;

import org.nexo.interactionservice.model.CommentModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ICommentRepository extends JpaRepository<CommentModel, Long> {
    Page<CommentModel> findByPostIdAndParentComment(Long postId, Pageable pageable, CommentModel commentModel);

    Page<CommentModel> findByReelId(Long reelId, Pageable pageable);

    Page<CommentModel> findByParentCommentId(Long postId, Pageable pageable);

}
