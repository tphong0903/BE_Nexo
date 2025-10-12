package org.nexo.userservice.repository;

import org.nexo.userservice.enums.EStatusFollow;
import org.nexo.userservice.model.FollowId;
import org.nexo.userservice.model.FollowModel;
import org.nexo.userservice.model.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FollowRepository extends JpaRepository<FollowModel, FollowId> {
        boolean existsById(FollowId id);

        Optional<FollowModel> findById(FollowId id);

        List<FollowModel> findAllByFollowerAndStatus(UserModel follower, EStatusFollow status);

        default List<FollowModel> findAllByFollower(UserModel follower) {
                return findAllByFollowerAndStatus(follower, EStatusFollow.ACTIVE);
        }

        List<FollowModel> findAllByFollowingAndStatus(UserModel following, EStatusFollow status);

        Page<FollowModel> findAllByFollowingAndStatus(UserModel following, EStatusFollow status, Pageable pageable);

        List<FollowModel> findAllByFollowingId(Long id);

        List<FollowModel> findAllByFollowerId(Long id);

        Page<FollowModel> findAllByFollowerId(Long id, Pageable pageable);

        default List<FollowModel> findAllByFollowing(UserModel following) {
                return findAllByFollowingAndStatus(following, EStatusFollow.ACTIVE);
        }

        default List<FollowModel> findAllByFollowingRequest(UserModel following) {
                return findAllByFollowingAndStatus(following, EStatusFollow.PENDING);
        }

        Optional<FollowModel> findByIdAndStatus(FollowId id, EStatusFollow status);

        List<FollowModel> findAllByFollowerAndIsCloseFriendAndStatus(UserModel follower, Boolean isCloseFriend,
                        EStatusFollow status);

        Page<FollowModel> findAllByFollowerAndIsCloseFriendAndStatus(UserModel follower, Boolean isCloseFriend,
                        EStatusFollow status, Pageable pageable);

        default List<FollowModel> findAllCloseFriendsByFollower(UserModel follower) {
                return findAllByFollowerAndIsCloseFriendAndStatus(follower, true, EStatusFollow.ACTIVE);
        }

        @Query("SELECT f.following.id FROM FollowModel f " +
                        "WHERE f.follower.id = :followerId AND f.status = :status")
        Set<Long> findAllFollowingIdsByFollowerIdAndStatus(@Param("followerId") Long followerId,
                        @Param("status") EStatusFollow status);

        boolean existsByFollowerIdAndFollowingIdAndStatus(Long followerId,
                        Long followingId,
                        EStatusFollow status);

        @Query("SELECT f FROM FollowModel f " +
                        "WHERE f.follower.id = :userId " +
                        "AND f.status = 'ACTIVE' " +
                        "AND EXISTS (" +
                        "   SELECT 1 FROM FollowModel f2 " +
                        "   WHERE f2.follower.id = f.following.id " +
                        "   AND f2.following.id = :userId " +
                        "   AND f2.status = 'ACTIVE'" +
                        ")")
        Page<FollowModel> findMutualFollowers(@Param("userId") Long userId, Pageable pageable);

        void deleteByFollowerIdAndFollowingId(Long currentUserId, Long targetUserId);

        @Query("SELECT f FROM FollowModel f " +
                        "WHERE f.following = :following " +
                        "AND f.status = :status " +
                        "AND (LOWER(f.follower.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(f.follower.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<FollowModel> findAllByFollowingAndStatusWithSearch(@Param("following") UserModel following,
                        @Param("status") EStatusFollow status,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT f FROM FollowModel f " +
                        "WHERE f.follower.id = :followerId " +
                        "AND f.status = 'ACTIVE' " +
                        "AND (LOWER(f.following.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(f.following.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<FollowModel> findAllByFollowerIdWithSearch(@Param("followerId") Long followerId,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT f FROM FollowModel f " +
                        "WHERE f.follower = :follower " +
                        "AND f.isCloseFriend = :isCloseFriend " +
                        "AND f.status = :status " +
                        "AND (LOWER(f.following.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(f.following.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<FollowModel> findAllByFollowerAndIsCloseFriendAndStatusWithSearch(@Param("follower") UserModel follower,
                        @Param("isCloseFriend") Boolean isCloseFriend,
                        @Param("status") EStatusFollow status,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT f FROM FollowModel f " +
                        "WHERE f.follower.id = :userId " +
                        "AND f.status = 'ACTIVE' " +
                        "AND (LOWER(f.following.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "OR LOWER(f.following.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        "AND EXISTS (" +
                        "   SELECT 1 FROM FollowModel f2 " +
                        "   WHERE f2.follower.id = f.following.id " +
                        "   AND f2.following.id = :userId " +
                        "   AND f2.status = 'ACTIVE'" +
                        ")")
        Page<FollowModel> findMutualFollowersWithSearch(@Param("userId") Long userId,
                        @Param("search") String search,
                        Pageable pageable);
}