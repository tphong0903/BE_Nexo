package org.nexo.userservice.service;

import org.nexo.userservice.dto.FolloweeDTO;

import java.util.List;
import java.util.Set;

public interface FollowService {
    void addFollow(String accessToken, String username);

    void acceptFollowRequest(String accessToken, String username);

    void rejectFollowRequest(String accessToken, String username);

    void removeFollow(String accessToken, String username);

    void toggleCloseFriend(String accessToken, String username);

    Set<FolloweeDTO> getFollowers(String username);

    Set<FolloweeDTO> getFollowings(String username);

    Set<FolloweeDTO> getFollowRequests(String accessToken);

    Set<FolloweeDTO> getFollowersByUserId(Long userId);

    Set<FolloweeDTO> getFollowingsByUserId(Long userId);

    List<Boolean> isFollow(Long userId1, Long userId2);

}