package org.nexo.interactionservice.repository;

import org.nexo.interactionservice.model.CommentMentionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ICommentMentionRepository extends JpaRepository<CommentMentionModel, Long> {
    @Transactional
    @Modifying
    @Query("DELETE FROM CommentMentionModel item WHERE (item.commentModel.id = :commentId) ")
    void deleteByCommentId(@Param("commentId") Long commentId);

    List<CommentMentionModel> findAllByCommentModelId(Long id);
}
