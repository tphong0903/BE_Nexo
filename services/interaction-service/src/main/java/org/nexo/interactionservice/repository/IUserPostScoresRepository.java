package org.nexo.interactionservice.repository;

import org.nexo.interactionservice.model.UserPostScores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IUserPostScoresRepository extends JpaRepository<UserPostScores, Long> {
    UserPostScores findByUserIdAndPostId(Long userId, Long postId);

    UserPostScores findByUserIdAndReelId(Long userId, Long reelId);
}
