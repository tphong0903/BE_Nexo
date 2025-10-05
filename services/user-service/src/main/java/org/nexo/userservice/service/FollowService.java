package org.nexo.userservice.service;

import org.nexo.userservice.dto.FolloweeDTO;
import org.nexo.userservice.dto.PageModelResponse;
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

    PageModelResponse<FolloweeDTO> getFollowers(String username, Pageable pageable, String accessToken);

    PageModelResponse<FolloweeDTO> getFollowings(String username, Pageable pageable, String accessToken);

    PageModelResponse<FolloweeDTO> getFollowRequests(String accessToken, Pageable pageable);

    Set<FolloweeDTO> getFollowersByUserId(Long userId);

    Set<FolloweeDTO> getFollowingsByUserId(Long userId);

    List<Boolean> isFollow(Long userId1, Long userId2);

    PageModelResponse<FolloweeDTO> getCloseFriends(String accessToken, Pageable pageable);

    PageModelResponse<FolloweeDTO> getMutualFollowers(String accessToken, Pageable pageable);


}