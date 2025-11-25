package org.nexo.userservice.service;

import org.nexo.userservice.dto.ChangePasswordRequest;
import org.nexo.userservice.dto.UpdateUserRequest;
import org.nexo.userservice.dto.UserDTOResponse;
import org.nexo.userservice.dto.UserProfileDTOResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserProfileDTOResponse getUserProfile(String username, String accessToken);

    UserDTOResponse getUserProfileMe(String accessToken);

    UserDTOResponse updateUser(String accessToken, UpdateUserRequest request, MultipartFile avatarFile);

    void deleteAvatar(String accessToken);

    void assignRoleToUser(String username, String role);

    void banUser(String username);
    
    void unbanUser(String username);

    void updateUserOauth(String keycloakUserId, UpdateUserRequest request);

    void changePassword(String accessToken, ChangePasswordRequest request);

    org.nexo.userservice.dto.UserStatisticsResponse getUserStatistics(Long userId);
}