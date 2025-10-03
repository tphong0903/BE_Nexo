package org.nexo.userservice.service.Impl;

import org.nexo.userservice.dto.UpdateUserRequest;
import org.nexo.userservice.dto.UserDTOResponse;
import org.nexo.userservice.exception.ResourceNotFoundException;
import org.nexo.userservice.grpc.UploadFileGrpcClient;
import org.nexo.userservice.mapper.UserMapper;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.UserRepository;
import org.nexo.userservice.service.UserService;
import org.nexo.userservice.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImp implements UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final UploadFileGrpcClient uploadFileGrpcClient;

    public UserDTOResponse getUserProfile(String username, String accessToken) {
        UserModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return userMapper.toUserDTOResponse(user);
    }

    public UserDTOResponse getUserProfileMe(String accessToken) {
        String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);
        UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + keycloakUserId));
        return userMapper.toUserDTOResponse(user);
    }

    @Transactional
    public UserDTOResponse updateUser(String accessToken, UpdateUserRequest request, MultipartFile avatarFile) {
        String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);
        UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + keycloakUserId));

        if (avatarFile != null && !avatarFile.isEmpty()) {
            log.info("Uploading avatar for user: {}", user.getUsername());
            String avatarUrl = uploadFileGrpcClient.uploadAvatar(avatarFile);
            if (avatarUrl != null) {
                request.setAvatar(avatarUrl);
                log.info("Avatar uploaded successfully: {}", avatarUrl);
            } else {
                log.warn("Failed to upload avatar, keeping existing avatar");
            }
        }

        UserModel updatedUser = userMapper.updateUserModelFromDTO(request, user);
        userRepository.save(updatedUser);
        return userMapper.toUserDTOResponse(updatedUser);
    }

}
