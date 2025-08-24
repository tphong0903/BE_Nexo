package org.nexo.postservice.repository;

import org.nexo.postservice.model.ReelModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IReelRepository extends JpaRepository<ReelModel, Long> {
}
