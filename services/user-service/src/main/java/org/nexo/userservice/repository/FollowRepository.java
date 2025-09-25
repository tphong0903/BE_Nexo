package org.nexo.userservice.repository;

import org.nexo.userservice.enums.EStatusFollow;
import org.nexo.userservice.model.FollowId;
import org.nexo.userservice.model.FollowModel;
import org.nexo.userservice.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<FollowModel, FollowId> {
    boolean existsById(FollowId id);

    Optional<FollowModel> findById(FollowId id);

    List<FollowModel> findAllByFollowerAndStatus(UserModel follower, EStatusFollow status);

    default List<FollowModel> findAllByFollower(UserModel follower) {
        return findAllByFollowerAndStatus(follower, EStatusFollow.ACTIVE);
    }

    List<FollowModel> findAllByFollowingAndStatus(UserModel following, EStatusFollow status);

    default List<FollowModel> findAllByFollowing(UserModel following) {
        return findAllByFollowingAndStatus(following, EStatusFollow.ACTIVE);
    }

    default List<FollowModel> findAllByFollowingRequest(UserModel following) {
        return findAllByFollowingAndStatus(following, EStatusFollow.PENDING);
    }

    Optional<FollowModel> findByIdAndStatus(FollowId id, EStatusFollow status);

}