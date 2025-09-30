package org.nexo.userservice.service;

import org.nexo.userservice.dto.FolloweeDTO;

import java.util.List;
import java.util.Set;

public interface FollowService {
    void addFollow(String accessToken, Long followingId);

    void acceptFollowRequest(String accessToken, Long followerId);

    void rejectFollowRequest(String accessToken, Long followerId);

    void removeFollow(String accessToken, Long followingId);

    void toggleCloseFriend(String accessToken, Long followingId);

    Set<FolloweeDTO> getFollowees(Long accessToken);

    Set<FolloweeDTO> getFollowings(Long accessToken);

    Set<FolloweeDTO> getFollowRequests(String accessToken);

    Set<FolloweeDTO> getFolloweesByUserId(Long userId);

    Set<FolloweeDTO> getFollowingsByUserId(Long userId);

    List<Boolean> isFollow(Long userId1, Long userId2);

}