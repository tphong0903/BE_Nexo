package org.nexo.userservice.repository;

import org.nexo.userservice.enums.EAccountStatus;
import org.nexo.userservice.enums.ERole;
import org.nexo.userservice.dto.RecommendationUserExportDTO;
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
    List<UserModel> findAllByAccountStatusAndRole(EAccountStatus status,ERole role);

    Optional<UserModel> findByUsername(String username);
    

    Optional<UserModel> findByEmail(String email);

    Optional<UserModel> findByEmailAndAccountStatus(String email, EAccountStatus status);

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

    Optional<UserModel> findFirstByUsernameAndAccountStatus(String username, EAccountStatus status);

    default Optional<UserModel> findActiveByUsername(String username) {
        return findFirstByUsernameAndAccountStatus(username, EAccountStatus.ACTIVE);
    }

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT DATE(u.createdAt) AS date, COUNT(u) AS total " +
            "FROM UserModel u " +
            "WHERE u.createdAt BETWEEN :start AND :end " +
            "GROUP BY DATE(u.createdAt) " +
            "ORDER BY DATE(u.createdAt)")
    List<Object[]> countUsersByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT " +
            "u.id AS id, " +
            "u.username AS username, " +
            "u.fullName AS fullName, " +
            "u.bio AS bio, " +
            "u.isPrivate AS isPrivate, " +
            "u.onlineStatus AS onlineStatus, " +
            "u.accountStatus AS accountStatus, " +
            "u.createdAt AS createdAt, " +
            "u.lastLogin AS lastActiveAt, " +
            "COALESCE((SELECT SUM(CASE " +
            "                    WHEN UPPER(l1.action) IN ('POST_CREATED', 'CREATE_POST') THEN 3.0 " +
            "                    WHEN UPPER(l1.action) = 'COMMENT_CREATED' THEN 2.0 " +
            "                    WHEN UPPER(l1.action) = 'POST_LIKED' THEN 1.0 " +
            "                    ELSE 0.5 END) " +
            "          FROM UserActivityLogModel l1 " +
            "          WHERE l1.user.id = u.id " +
            "          AND l1.createdAt >= :activitySince), 0.0) AS activityScore, " +
            "COALESCE((SELECT CAST(COUNT(l2.id) AS double) " +
            "          FROM UserActivityLogModel l2 " +
            "          WHERE l2.user.id = u.id " +
            "          AND l2.createdAt >= :postSince " +
            "          AND UPPER(l2.action) IN ('POST_CREATED', 'CREATE_POST')), 0.0) / 7.0 AS postFrequency, " +
            "COALESCE((SELECT CAST(COUNT(f1.id) AS double) " +
            "          FROM FollowModel f1 " +
            "          WHERE f1.follower.id = u.id " +
            "          AND f1.status = org.nexo.userservice.enums.EStatusFollow.ACTIVE " +
            "          AND EXISTS (SELECT 1 FROM FollowModel f2 " +
            "                      WHERE f2.follower.id = f1.following.id " +
            "                      AND f2.following.id = u.id " +
            "                      AND f2.status = org.nexo.userservice.enums.EStatusFollow.ACTIVE)), 0.0) AS mutualInteractions " +
            "FROM UserModel u")
    List<RecommendationUserExportDTO> findAllForRecommendationExport(
            @Param("activitySince") LocalDateTime activitySince,
            @Param("postSince") LocalDateTime postSince);

}
