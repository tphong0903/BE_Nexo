package org.nexo.postservice.repository;

import org.nexo.postservice.model.StoryModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IStoryRepository extends JpaRepository<StoryModel, Long> {
    Page<StoryModel> findByUserIdAndIsActive(Long userId, Boolean isActive, Pageable pageable);

    Page<StoryModel> findByUserId(Long userId, Pageable pageable);


    List<StoryModel> findByUserIdAndIsActiveAndIsClosedFriend(Long userId, Boolean isActive, Boolean isClosedFriend);
}
