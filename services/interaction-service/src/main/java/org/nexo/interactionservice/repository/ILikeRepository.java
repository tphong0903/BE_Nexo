package org.nexo.interactionservice.repository;

import org.nexo.interactionservice.model.LikeModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ILikeRepository extends JpaRepository<LikeModel, Long> {
    LikeModel findByPostIdAndUserId(Long postId, Long userId);

    Boolean existsByPostIdAndUserId(Long postId, Long userId);

    Boolean existsByReelIdAndUserId(Long reelId, Long userId);

    LikeModel findByReelIdAndUserId(Long reelId, Long userId);

}
