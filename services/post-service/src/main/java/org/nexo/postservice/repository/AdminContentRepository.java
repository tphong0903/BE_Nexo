package org.nexo.postservice.repository;

import org.nexo.postservice.dto.response.ContentProjection;
import org.nexo.postservice.model.PostModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminContentRepository extends JpaRepository<PostModel, Long> {

    @Query(nativeQuery = true, value = """
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
                WHERE (:search IS NULL OR caption ILIKE CONCAT('%', :search, '%') OR author_name ILIKE CONCAT('%', :search, '%'))
                
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
                WHERE (:search IS NULL OR caption ILIKE CONCAT('%', :search, '%') OR author_name ILIKE CONCAT('%', :search, '%'))
                
            ) AS combined_content
            WHERE (:type = 'all' OR type = :type)
            """,
            countQuery = """
                    SELECT count(*) FROM (
                        SELECT id, 'post' as type, caption as content, author_name FROM post_model
                        WHERE (:search IS NULL OR caption ILIKE CONCAT('%', :search, '%') OR author_name ILIKE CONCAT('%', :search, '%'))
                        UNION ALL
                        SELECT id, 'reel' as type, caption as content, author_name FROM reel_model
                        WHERE (:search IS NULL OR caption ILIKE CONCAT('%', :search, '%') OR author_name ILIKE CONCAT('%', :search, '%'))
                    ) as combined_count
                    WHERE (:type = 'all' OR type = :type)
                    """)
    Page<ContentProjection> findAllContent(@Param("search") String search,
                                           @Param("type") String type,
                                           Pageable pageable);
}