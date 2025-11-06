package org.nexo.postservice.repository;

import org.nexo.postservice.model.PostModel;
import org.nexo.postservice.util.Enum.EVisibilityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IPostRepository extends JpaRepository<PostModel, Long> {
    Page<PostModel> findByUserIdAndIsActive(Long id, Boolean isActive, Pageable pageable);

    Page<PostModel> findByUserIdAndIsActiveAndVisibility(Long id, Boolean isActive, EVisibilityPost status, Pageable pageable);

    @Query(
            value = """
                    SELECT p.* FROM post_model p
                    LEFT JOIN post_hash_tag_model ph ON p.id = ph.post_id
                    LEFT JOIN hash_tag_model h ON ph.hashtag_id = h.id AND h.is_active = true
                    WHERE p.visibility = 'PUBLIC' AND p.is_active = true
                    GROUP BY p.id
                    ORDER BY COALESCE(SUM(h.usage_count), 0) DESC, (p.like_quantity + p.comment_quantity * 2) DESC, p.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM post_model p
                    WHERE p.visibility = 'PUBLIC' AND p.is_active = true
                    """
            , nativeQuery = true
    )
    Page<PostModel> findPopularPublicPostsWithHashtagScore(Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT DATE(p.createdAt) AS date, COUNT(p) AS total " +
            "FROM PostModel p " +
            "WHERE p.createdAt BETWEEN :start AND :end " +
            "GROUP BY DATE(p.createdAt) " +
            "ORDER BY DATE(p.createdAt)")
    List<Object[]> countPostsByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

}
