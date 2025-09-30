package org.nexo.feedservice.repository;

import org.nexo.feedservice.model.FeedModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IFeedRepository extends JpaRepository<FeedModel, Long> {
    @Query("SELECT f.postId FROM FeedModel f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    List<Long> findPostIdsByUserId(@Param("userId") Long userId, Pageable pageable);
}
