
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

import java.util.List;

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

        @PostMapping
        public ResponseData<?> sendMessage(@RequestBody ReplyStoryRequsestDTO request, Authentication authentication) {
                String keycloakUserId = (String) authentication.getName();
                UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
                Long userId = userDto.getUserId();
                MessageDTO message = messageService.replyStory(request, userId);
                return ResponseData.builder()
                                .status(200)
                                .message("Message sent successfully")
                                .data(message)
                                .build();
        }

        @GetMapping("/{messageId}/reactions")
        public ResponseData<?> getMessageReactions(
                        @PathVariable Long messageId,
                        Authentication authentication) {

                String keycloakUserId = (String) authentication.getName();

                UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
                Long userId = userDto.getUserId();

                List<ReactionDetailDTO> reactions = messageService.getMessageReactions(messageId, userId);

                return ResponseData.builder()
                                .status(200)
                                .message("Reactions retrieved successfully")
                                .data(reactions)
                                .build();
        }
}