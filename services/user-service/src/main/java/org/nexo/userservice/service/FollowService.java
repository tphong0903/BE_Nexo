package org.nexo.userservice.service;

import org.nexo.userservice.dto.FolloweeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface FollowService {
    void addFollow(String accessToken, String username);

    void acceptFollowRequest(String accessToken, String username);

    void rejectFollowRequest(String accessToken, String username);

    void removeFollow(String accessToken, String username);

    void toggleCloseFriend(String accessToken, String username);


    Page<FolloweeDTO> getFollowers(String username, Pageable pageable);


    Page<FolloweeDTO> getFollowings(String username, Pageable pageable);


    Page<FolloweeDTO> getFollowRequests(String accessToken, Pageable pageable);

    Set<FolloweeDTO> getFollowersByUserId(Long userId);

    Set<FolloweeDTO> getFollowingsByUserId(Long userId);

    List<Boolean> isFollow(Long userId1, Long userId2);

    Set<FolloweeDTO> getCloseFriends(String accessToken);

    Page<FolloweeDTO> getCloseFriends(String accessToken, Pageable pageable);

}