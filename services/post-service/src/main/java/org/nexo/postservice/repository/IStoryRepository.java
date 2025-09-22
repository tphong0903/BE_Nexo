package org.nexo.postservice.repository;

import org.nexo.postservice.model.StoryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IStoryRepository extends JpaRepository<StoryModel, Long> {
    List<StoryModel> findByUserIdAndIsActive(Long userId, Boolean isActive);

    List<StoryModel> findByUserIdAndIsActiveAndIsClosedFriend(Long userId, Boolean isActive, Boolean isClosedFriend);
}
