package org.nexo.userservice.repository;

import org.nexo.userservice.model.UserBlockId;
import org.nexo.userservice.model.UserBlockModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlockModel, UserBlockId> {

    boolean existsByIdBlockerIdAndIdBlockedId(Long blockerId, Long blockedId);

    void deleteByIdBlockerIdAndIdBlockedId(Long blockerId, Long blockedId);

    Page<UserBlockModel> findByIdBlockerId(Long blockerId, Pageable pageable);
}