package org.nexo.userservice.repository;

import org.nexo.userservice.enums.EAccountStatus;
import org.nexo.userservice.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {

    Optional<UserModel> findByUsername(String username);

    Optional<UserModel> findByEmail(String email);

    Optional<UserModel> findByIdAndAccountStatus(Long id, EAccountStatus status);

    default Optional<UserModel> findActiveById(Long id) {
        return findByIdAndAccountStatus(id, EAccountStatus.ACTIVE);
    }

    Optional<UserModel> findByKeycloakUserId(String keycloakUserId);

    Optional<UserModel> findByKeycloakUserIdAndAccountStatus(String keycloakUserId, EAccountStatus status);

    default Optional<UserModel> findActiveByKeycloakUserId(String keycloakUserId) {
        return findByKeycloakUserIdAndAccountStatus(keycloakUserId, EAccountStatus.ACTIVE);
    }

    boolean existsByEmail(String email);

    boolean existsByKeycloakUserId(String keycloakUserId);

    Optional<UserModel> findByUsernameAndAccountStatus(String username, EAccountStatus status);

    default Optional<UserModel> findActiveByUsername(String username) {
        return findByUsernameAndAccountStatus(username, EAccountStatus.ACTIVE);
    }

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT DATE(u.createdAt) AS date, COUNT(u) AS total " +
            "FROM UserModel u " +
            "WHERE u.createdAt BETWEEN :start AND :end " +
            "GROUP BY DATE(u.createdAt) " +
            "ORDER BY DATE(u.createdAt)")
    List<Object[]> countUsersByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

}
