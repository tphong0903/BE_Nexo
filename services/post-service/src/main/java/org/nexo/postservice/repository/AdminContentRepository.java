package org.nexo.postservice.repository;

import org.nexo.postservice.dto.response.ContentProjection;
import org.nexo.postservice.model.PostModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AdminContentRepository extends JpaRepository<PostModel, Long> {

    @Query(
            nativeQuery = true,
            value = """
                    SELECT * FROM (
                        SELECT
                            id,
                            CAST(gen_random_uuid() AS TEXT) as uuid,
                            author_name,
                            caption,
                            'post' as type,
                            user_id,
                            like_quantity,
                            comment_quantity,
                            visibility,
                            created_at,
                            is_active
                        FROM post_model
                        WHERE (:search IS NULL OR caption ILIKE CONCAT('%', :search, '%')
                               OR author_name ILIKE CONCAT('%', :search, '%'))
                        AND (:hashtag IS NULL OR caption ILIKE CONCAT('%#', :hashtag, '%'))
                        AND (:content IS NULL OR caption ILIKE CONCAT('%', :content, '%'))
                        AND (:authorName IS NULL OR author_name ILIKE CONCAT('%', :authorName, '%'))
                        AND created_at >= COALESCE(CAST(:startDate AS TIMESTAMP), created_at)
                        AND created_at <= COALESCE(CAST(:endDate AS TIMESTAMP), created_at)

                        UNION ALL

                        SELECT
                            id,
                            CAST(gen_random_uuid() AS TEXT) as uuid,
                            author_name,
                            caption,
                            'reel' as type,
                            user_id,
                            like_quantity,
                            comment_quantity,
                            visibility,
                            created_at,
                            is_active
                        FROM reel_model
                        WHERE (:search IS NULL OR caption ILIKE CONCAT('%', :search, '%')
                               OR author_name ILIKE CONCAT('%', :search, '%'))
                        AND (:hashtag IS NULL OR caption ILIKE CONCAT('%#', :hashtag, '%'))
                        AND (:content IS NULL OR caption ILIKE CONCAT('%', :content, '%'))
                        AND (:authorName IS NULL OR author_name ILIKE CONCAT('%', :authorName, '%'))
                        AND created_at >= COALESCE(CAST(:startDate AS TIMESTAMP), created_at)
                        AND created_at <= COALESCE(CAST(:endDate AS TIMESTAMP), created_at)

                    ) AS combined_content
                    WHERE (:type = 'all' OR type = :type)
                    ORDER BY created_at DESC
                    """,
            countQuery = """
                    SELECT count(*) FROM (
                        SELECT id, 'post' as type
                        FROM post_model
                        WHERE (:search IS NULL OR caption ILIKE CONCAT('%', :search, '%')
                               OR author_name ILIKE CONCAT('%', :search, '%'))
                        AND (:hashtag IS NULL OR caption ILIKE CONCAT('%#', :hashtag, '%'))
                        AND (:content IS NULL OR caption ILIKE CONCAT('%', :content, '%'))
                        AND (:authorName IS NULL OR author_name ILIKE CONCAT('%', :authorName, '%'))
                        AND created_at >= COALESCE(CAST(:startDate AS TIMESTAMP), created_at)
                        AND created_at <= COALESCE(CAST(:endDate AS TIMESTAMP), created_at)

                        UNION ALL

                        SELECT id, 'reel' as type
                        FROM reel_model
                        WHERE (:search IS NULL OR caption ILIKE CONCAT('%', :search, '%')
                               OR author_name ILIKE CONCAT('%', :search, '%'))
                        AND (:hashtag IS NULL OR caption ILIKE CONCAT('%#', :hashtag, '%'))
                        AND (:content IS NULL OR caption ILIKE CONCAT('%', :content, '%'))
                        AND (:authorName IS NULL OR author_name ILIKE CONCAT('%', :authorName, '%'))
                        AND created_at >= COALESCE(CAST(:startDate AS TIMESTAMP), created_at)
                        AND created_at <= COALESCE(CAST(:endDate AS TIMESTAMP), created_at)
                    ) as combined_count
                    WHERE (:type = 'all' OR type = :type)
                    """
    )
    Page<ContentProjection> findAllContent(
            @Param("search") String search,
            @Param("type") String type,
            @Param("hashtag") String hashtag,
            @Param("content") String content,
            @Param("authorName") String authorName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}