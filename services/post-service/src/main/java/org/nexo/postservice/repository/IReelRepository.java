package org.nexo.postservice.repository;

import jakarta.transaction.Transactional;
import org.nexo.postservice.model.ReelModel;
import org.nexo.postservice.util.Enum.EVisibilityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface IReelRepository extends JpaRepository<ReelModel, Long> {
    Page<ReelModel> findByUserIdAndIsActive(Long id, Boolean isActive, Pageable pageable);

    Page<ReelModel> findByUserId(Long id, Pageable pageable);

    long countByUserIdAndIsActive(Long userId, Boolean isActive);

    @Modifying
    @Transactional
    @Query("UPDATE ReelModel p SET p.likeQuantity = p.likeQuantity + :count WHERE p.id = :id")
    void updateLikeQuantity(@Param("id") Long id, @Param("count") int count);

    @Modifying
    @Transactional
    @Query("UPDATE ReelModel p SET p.commentQuantity = p.commentQuantity + :count WHERE p.id = :id")
    void updateCommentQuantity(@Param("id") Long id, @Param("count") int count);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Page<ReelModel> findByUserIdAndIsActiveAndVisibility(Long id, Boolean isActive, EVisibilityPost status,
                                                         Pageable pageable);
}
