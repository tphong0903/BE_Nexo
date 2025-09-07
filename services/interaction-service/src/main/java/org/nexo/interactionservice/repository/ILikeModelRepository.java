package org.nexo.interactionservice.repository;

import org.nexo.interactionservice.model.LikeModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ILikeModelRepository extends JpaRepository<LikeModel, Long> {
}
