package org.nexo.postservice.repository;

import org.nexo.postservice.model.StoryViewModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IStoryViewRepository extends JpaRepository<StoryViewModel, Long> {
    Optional<StoryViewModel> findByStoryModel_IdAndSeenUserId(Long storyId, Long seenUserId);
}
