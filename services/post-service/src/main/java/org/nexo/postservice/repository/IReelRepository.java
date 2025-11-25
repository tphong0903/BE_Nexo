package org.nexo.postservice.repository;

import org.nexo.postservice.model.PostModel;
import org.nexo.postservice.model.ReelModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IReelRepository extends JpaRepository<ReelModel, Long> {
    Page<ReelModel> findByUserIdAndIsActive(Long id, Boolean isActive, Pageable pageable);

    Page<ReelModel> findByUserId(Long id, Pageable pageable);

    long countByUserIdAndIsActive(Long userId, Boolean isActive);
}
