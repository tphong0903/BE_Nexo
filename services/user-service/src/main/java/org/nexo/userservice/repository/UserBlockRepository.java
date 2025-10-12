package org.nexo.userservice.repository;

import org.nexo.userservice.model.UserBlockId;
import org.nexo.userservice.model.UserBlockModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlockModel, UserBlockId> {

    boolean existsByIdBlockerIdAndIdBlockedId(Long blockerId, Long blockedId);

    void deleteByIdBlockerIdAndIdBlockedId(Long blockerId, Long blockedId);

    Page<UserBlockModel> findByIdBlockerId(Long blockerId, Pageable pageable);

    @Query("SELECT ub FROM UserBlockModel ub " +
            "JOIN UserModel u ON ub.id.blockedId = u.id " +
            "WHERE ub.id.blockerId = :blockerId " +
            "AND (:search IS NULL OR :search = '' OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<UserBlockModel> findByIdBlockerIdWithSearch(@Param("blockerId") Long blockerId,
            @Param("search") String search,
            Pageable pageable);
}