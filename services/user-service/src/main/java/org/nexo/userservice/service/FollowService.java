package org.nexo.userservice.service;

import java.util.Set;
import org.nexo.userservice.dto.FolloweeDTO;

public interface FollowService {
    void addFollow(String accessToken, Long followingId);

    void removeFollow(String accessToken, Long followingId);

    void toggleCloseFriend(String accessToken, Long followingId);

    Set<FolloweeDTO> getFollowees(String accessToken);

    Set<FolloweeDTO> getFolloweesByUserId(Long userId);

}