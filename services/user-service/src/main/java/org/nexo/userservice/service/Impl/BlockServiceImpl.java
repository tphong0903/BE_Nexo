package org.nexo.userservice.service.Impl;

import java.util.List;
import java.util.stream.Collectors;

import org.nexo.userservice.dto.PageModelResponse;
import org.nexo.userservice.dto.UserDTOResponse;
import org.nexo.userservice.exception.ResourceNotFoundException;
import org.nexo.userservice.grpc.MessagingGrpcClient;
import org.nexo.userservice.mapper.UserMapper;
import org.nexo.userservice.model.UserBlockId;
import org.nexo.userservice.model.UserBlockModel;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.FollowRepository;
import org.nexo.userservice.repository.UserBlockRepository;
import org.nexo.userservice.repository.UserRepository;
import org.nexo.userservice.service.BlockService;
import org.nexo.userservice.util.JwtUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockServiceImpl implements BlockService {

        private final UserBlockRepository userBlockRepository;
        private final UserRepository userRepository;
        private final JwtUtil jwtUtil;
        private final UserMapper userMapper;
        private final FollowRepository followRepository;
        private final MessagingGrpcClient messagingGrpcClient;

        public void block(Long blockerId, Long blockedId) {
                if (blockerId.equals(blockedId)) {
                        throw new ResourceNotFoundException("Cannot block yourself");
                }

                UserBlockId blockId = UserBlockId.builder()
                                .blockerId(blockerId)
                                .blockedId(blockedId)
                                .build();

                if (userBlockRepository.existsById(blockId)) {
                        throw new ResourceNotFoundException("User is already blocked");
                }

                UserBlockModel blockModel = UserBlockModel.builder()
                                .id(blockId)
                                .build();

                userBlockRepository.save(blockModel);
                log.info("User {} blocked user {}", blockerId, blockedId);
        }

        @Transactional
        public void unblock(Long blockerId, Long blockedId) {
                UserBlockId blockId = UserBlockId.builder()
                                .blockerId(blockerId)
                                .blockedId(blockedId)
                                .build();

                if (!userBlockRepository.existsById(blockId)) {
                        throw new ResourceNotFoundException("User is not blocked");
                }

                userBlockRepository.deleteById(blockId);
                log.info("User {} unblocked user {}", blockerId, blockedId);
        }

        public boolean isBlocked(Long blockerId, Long blockedId) {
                UserBlockId blockId = UserBlockId.builder()
                                .blockerId(blockerId)
                                .blockedId(blockedId)
                                .build();
                return userBlockRepository.existsById(blockId);
        }

        @Transactional
        public void blockUser(String token, String username) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(token);
                Long currentUserId = userRepository.findActiveByKeycloakUserId(keycloakUserId)
                                .map(UserModel::getId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                Long targetUserId = userRepository.findByUsername(username)
                                .map(UserModel::getId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Target user not found with username: " + username));
                userRepository.findById(targetUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));
                block(currentUserId, targetUserId);
                followRepository.deleteByFollowerIdAndFollowingId(currentUserId, targetUserId);
                messagingGrpcClient.handleBlockStatusChange(currentUserId, targetUserId, true);
        }

        @Transactional
        public void unblockUser(String token, String username) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(token);
                Long currentUserId = userRepository.findActiveByKeycloakUserId(keycloakUserId)
                                .map(UserModel::getId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                Long targetUserId = userRepository.findByUsername(username)
                                .map(UserModel::getId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Target user not found with username: " + username));
                userRepository.findById(targetUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));
                unblock(currentUserId, targetUserId);

                messagingGrpcClient.handleBlockStatusChange(currentUserId, targetUserId, false);

        }

        @Override
        public PageModelResponse<UserDTOResponse> getBlockedUsers(String accessToken, Pageable pageable,
                        String search) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);
                Long currentUserId = userRepository.findActiveByKeycloakUserId(keycloakUserId)
                                .map(UserModel::getId)
                                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

                Page<UserBlockModel> blockedPage;
                if (search != null && !search.trim().isEmpty()) {
                        blockedPage = userBlockRepository.findByIdBlockerIdWithSearch(currentUserId, search.trim(),
                                        pageable);
                } else {
                        blockedPage = userBlockRepository.findByIdBlockerId(currentUserId, pageable);
                }

                List<UserDTOResponse> blockedUserDTOs = blockedPage.getContent().stream()
                                .map(blockModel -> {
                                        UserModel blockedUser = userRepository
                                                        .findById(blockModel.getId().getBlockedId())
                                                        .orElse(null);
                                        return blockedUser != null ? userMapper.toUserDTOResponse(blockedUser) : null;
                                })
                                .filter(dto -> dto != null)
                                .collect(Collectors.toList());

                return PageModelResponse.<UserDTOResponse>builder()
                                .content(blockedUserDTOs)
                                .pageNo(blockedPage.getNumber())
                                .pageSize(blockedPage.getSize())
                                .totalElements(blockedPage.getTotalElements())
                                .totalPages(blockedPage.getTotalPages())
                                .last(blockedPage.isLast())
                                .build();
        }
}
