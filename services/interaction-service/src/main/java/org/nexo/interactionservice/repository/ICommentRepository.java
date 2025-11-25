package org.nexo.interactionservice.repository;

import org.nexo.interactionservice.model.CommentModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ICommentRepository extends JpaRepository<CommentModel, Long> {
    Page<CommentModel> findByPostIdAndParentComment(Long postId, Pageable pageable, CommentModel commentModel);

    Page<CommentModel> findByReelIdAndParentComment(Long reelId, Pageable pageable, CommentModel commentModel);

    Page<CommentModel> findByParentCommentId(Long postId, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT DATE(u.createdAt) AS date, COUNT(u) AS total " +
            "FROM CommentModel u " +
            "WHERE u.createdAt BETWEEN :start AND :end " +
            "GROUP BY DATE(u.createdAt) " +
            "ORDER BY DATE(u.createdAt)")
    List<Object[]> countCommentsByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByUserId(Long userId);

}
