package org.nexo.feedservice.repository;

import org.nexo.feedservice.model.FeedReelModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IFeedReelRepository extends JpaRepository<FeedReelModel, Long> {
    @Query("SELECT f.reelId FROM FeedReelModel f WHERE f.followerId = :followerId ORDER BY f.createdAt DESC")
    Page<Long> findReelIdsByFollowerId(@Param("followerId") Long followerId, Pageable pageable);

    long countFeedReelModelsByFollowerId(Long userId);
}
