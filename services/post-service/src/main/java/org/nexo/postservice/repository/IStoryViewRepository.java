package org.nexo.postservice.repository;

import org.nexo.postservice.model.StoryViewModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IStoryViewRepository extends JpaRepository<StoryViewModel, Long> {
}
