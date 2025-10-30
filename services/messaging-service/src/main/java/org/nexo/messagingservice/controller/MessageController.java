package org.nexo.messagingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.nexo.grpc.user.UserServiceProto;
import org.nexo.messagingservice.dto.*;
import org.nexo.messagingservice.enums.EReactionType;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.service.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

        private final MessageService messageService;
        private final UserGrpcClient userGrpcClient;

        @GetMapping
        public ResponseData<?> getMessages(
                        @PageableDefault(size = 10, sort = "id") Pageable pageable,
                        @RequestParam Long conversationId,
                        @RequestParam(value = "search", required = false) String search,
                        Authentication authentication) {

                String keycloakUserId = (String) authentication.getName();

                UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
                Long userId = userDto.getUserId();

                Page<MessageDTO> messages = messageService
                                .getMessages(conversationId, pageable, userId, search);

                return ResponseData.builder()
                                .status(200)
                                .message("Messages retrieved successfully")
                                .data(messages)
                                .build();
        }

        @PostMapping("/{messageId}/reactions")
        public ResponseData<?> reactToMessage(
                        @PathVariable Long messageId,
                        @RequestBody ReactMessageRequest request,
                        Authentication authentication) {

                String keycloakUserId = (String) authentication.getName();

                UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
                Long userId = userDto.getUserId();

                messageService.addReaction(messageId, userId, request.getReactionType());

                return ResponseData.builder()
                                .status(200)
                                .message("Reaction added successfully")
                                .build();
        }

        @DeleteMapping("/{messageId}/reactions")
        public ResponseData<?> removeReaction(
                        @PathVariable Long messageId,
                        @RequestParam String reactionType,
                        Authentication authentication) {

                String keycloakUserId = (String) authentication.getName();

                UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
                Long userId = userDto.getUserId();

                messageService.removeReaction(
                                messageId,
                                userId,
                                EReactionType.valueOf(reactionType));

                return ResponseData.builder()
                                .status(200)
                                .message("Reaction removed successfully")
                                .build();
        }
}