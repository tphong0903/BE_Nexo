package org.nexo.userservice.repository;

import org.nexo.userservice.model.UserActivityLogModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLogModel, Long> {
}
