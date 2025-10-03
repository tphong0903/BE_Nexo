package org.nexo.userservice.service;

import org.nexo.userservice.dto.UpdateUserRequest;
import org.nexo.userservice.dto.UserDTOResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserDTOResponse getUserProfile(String username, String accessToken);

    UserDTOResponse getUserProfileMe(String accessToken);

    UserDTOResponse updateUser(String accessToken, UpdateUserRequest request, MultipartFile avatarFile);
}