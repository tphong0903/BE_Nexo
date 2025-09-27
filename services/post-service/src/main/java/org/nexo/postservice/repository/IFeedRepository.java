package org.nexo.postservice.repository;

import org.nexo.postservice.model.FeedModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IFeedRepository extends JpaRepository<FeedModel, Long> {
}
