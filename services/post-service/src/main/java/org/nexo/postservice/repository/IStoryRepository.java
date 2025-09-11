package org.nexo.postservice.repository;

import org.nexo.postservice.model.StoryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IStoryRepository extends JpaRepository<StoryModel, Long> {
}
