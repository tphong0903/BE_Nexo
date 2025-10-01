package org.nexo.userservice.service;

import org.nexo.userservice.dto.UpdateUserRequest;
import org.nexo.userservice.dto.UserDTOResponse;

public interface UserService {

    UserDTOResponse getUserProfile(Long userId, String accessToken);
    
    UserDTOResponse getUserProfileMe(String accessToken);

    UserDTOResponse updateUser(String accessToken, UpdateUserRequest request);
}