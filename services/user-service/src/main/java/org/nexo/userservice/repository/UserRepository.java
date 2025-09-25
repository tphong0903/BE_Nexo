package org.nexo.userservice.repository;

import org.nexo.userservice.enums.EAccountStatus;
import org.nexo.userservice.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {
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

}
