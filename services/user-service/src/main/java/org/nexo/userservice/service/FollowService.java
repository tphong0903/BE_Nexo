package org.nexo.userservice.service;

import java.util.Set;

public interface FollowService {
    void addFollow(Long followerId, Long followingId, boolean isCloseFriend);

    void removeFollow(Long followerId, Long followingId);

    Set<String> getFollowees(Long userId);

}