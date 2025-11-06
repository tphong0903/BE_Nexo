package org.nexo.messagingservice.controller;

import lombok.RequiredArgsConstructor;

import org.nexo.grpc.user.UserServiceProto;
import org.nexo.messagingservice.dto.ResponseData;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.service.PresenceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {

        private final PresenceService presenceService;
        private final UserGrpcClient userGrpcClient;

        @GetMapping("/online/{targetUserId}")
        public ResponseData<?> isUserOnline(@PathVariable Long targetUserId, Authentication authentication) {
                String keycloakUserId = (String) authentication.getName();

                UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
                Long userId = userDto.getUserId();
                return ResponseData.builder()
                                .status(200)
                                .message("User online status retrieved successfully")
                                .data(presenceService.canSeeUserOnline(userId, targetUserId))
                                .build();
        }

        @PostMapping("/online/batch")
        public ResponseData<?> getUsersOnlineStatus(
                        @RequestBody List<Long> userIds,
                        Authentication authentication) {

                String keycloakUserId = (String) authentication.getName();

                UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
                Long userId = userDto.getUserId();
                Map<Long, Boolean> statuses = presenceService.getUsersOnlineStatus(
                                userId,
                                userIds);
                return ResponseData.builder()
                                .status(200)
                                .message("Users online status retrieved successfully")
                                .data(statuses)
                                .build();
        }

        @GetMapping("/online/friends")
        public ResponseData<?> getOnlineMutualFriends(
                        Authentication authentication) {

                String keycloakUserId = (String) authentication.getName();

                UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
                Long userId = userDto.getUserId();

                List<Long> onlineFriends = presenceService.getOnlineMutualFriends(userId);
                return ResponseData.builder()
                                .status(200)
                                .message("Online mutual friends retrieved successfully")
                                .data(onlineFriends)
                                .build();
        }

        @GetMapping("/last-seen/{targetUserId}")
        public ResponseData<?> getLastSeen(@PathVariable Long targetUserId, Authentication authentication) {
                String keycloakUserId = (String) authentication.getName();

                UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
                Long userId = userDto.getUserId();
                String lastSeen = presenceService.getLastSeen(userId, targetUserId);

                return ResponseData.builder()
                                .status(200)
                                .message("User last seen status retrieved successfully")
                                .data(lastSeen != null ? lastSeen : "Hidden")
                                .build();
        }

        @GetMapping("/online/count")
        public ResponseData<?> getOnlineUsersCount() {
                return ResponseData.builder()
                                .status(200)
                                .message("Online users count retrieved successfully")
                                .data(presenceService.getOnlineUsersCount())
                                .build();
        }

        @PostMapping("/clear-cache")
        public ResponseData<?> clearCache(Authentication authentication) {
                String keycloakUserId = (String) authentication.getName();

                UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
                Long userId = userDto.getUserId();
                presenceService.clearActivityStatusCache(userId);
                return ResponseData.builder()
                                .status(200)
                                .message("Activity status cache cleared successfully")
                                .build();
        }
}