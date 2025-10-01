package org.nexo.userservice.service.Impl;

import org.nexo.userservice.dto.UpdateUserRequest;
import org.nexo.userservice.dto.UserDTOResponse;
import org.nexo.userservice.exception.ResourceNotFoundException;
import org.nexo.userservice.mapper.UserMapper;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.UserRepository;
import org.nexo.userservice.service.UserService;
import org.nexo.userservice.util.JwtUtil;
import org.springframework.stereotype.Service;

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

    public UserDTOResponse getUserProfile(Long userId, String accessToken) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return userMapper.toUserDTOResponse(user);
    }

    public UserDTOResponse getUserProfileMe(String accessToken) {
        String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);
        UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + keycloakUserId));
        return userMapper.toUserDTOResponse(user);
    }

    @Transactional
    public UserDTOResponse updateUser(String accessToken, UpdateUserRequest request) {
        String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);
        UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + keycloakUserId));
        UserModel updatedUser = userMapper.updateUserModelFromDTO(request, user);
        userRepository.save(updatedUser);
        return userMapper.toUserDTOResponse(updatedUser);
    }

}
