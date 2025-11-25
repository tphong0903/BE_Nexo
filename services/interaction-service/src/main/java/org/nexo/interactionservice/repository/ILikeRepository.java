package org.nexo.interactionservice.repository;

import org.nexo.interactionservice.model.LikeModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ILikeRepository extends JpaRepository<LikeModel, Long> {
    LikeModel findByPostIdAndUserId(Long postId, Long userId);

    Boolean existsByPostIdAndUserId(Long postId, Long userId);

    Boolean existsByReelIdAndUserId(Long reelId, Long userId);

    LikeModel findByReelIdAndUserId(Long reelId, Long userId);

    Page<LikeModel> findByPostId(Long postId, Pageable pageable);

    Page<LikeModel> findByReelId(Long reelId, Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT DATE(u.createdAt) AS date, COUNT(u) AS total " +
            "FROM LikeModel u " +
            "WHERE u.createdAt BETWEEN :start AND :end " +
            "GROUP BY DATE(u.createdAt) " +
            "ORDER BY DATE(u.createdAt)")
    List<Object[]> countLikesByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByUserId(Long userId);

}
