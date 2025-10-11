package org.nexo.userservice.service;

import org.nexo.userservice.dto.PageModelResponse;
import org.nexo.userservice.dto.UserDTOResponse;
import org.springframework.data.domain.Pageable;

public interface BlockService {

    void blockUser(String token, String username);

    void unblockUser(String token, String username);

    boolean isBlocked(Long blockerId, Long blockedId);

    PageModelResponse<UserDTOResponse> getBlockedUsers(String accessToken, Pageable pageable);

}
