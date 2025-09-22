package org.nexo.userservice.repository;

import org.nexo.userservice.model.FollowId;
import org.nexo.userservice.model.FollowModel;
import org.nexo.userservice.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<FollowModel, FollowId> {
    boolean existsById(FollowId id);
    Optional<FollowModel> findById(FollowId id);

    List<FollowModel> findAllByFollower(UserModel follower);
    List<FollowModel> findAllByFollowing(UserModel following);
}