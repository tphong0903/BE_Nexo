package org.nexo.userservice.repository;

import org.nexo.userservice.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {
    Optional<UserModel> findByEmail(String email);

    Optional<UserModel> findByKeycloakUserId(String keycloakUserId);

    boolean existsByEmail(String email);

    boolean existsByKeycloakUserId(String keycloakUserId);



}
