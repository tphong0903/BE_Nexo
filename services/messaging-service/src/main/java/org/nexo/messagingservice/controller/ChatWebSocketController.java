package org.nexo.messagingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.nexo.grpc.user.UserServiceProto;
import org.nexo.messagingservice.dto.*;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.service.MessageService;
import org.nexo.messagingservice.service.PresenceService;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;
    private final UserGrpcClient userGrpcClient;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request,
            Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        String keycloakUserId = (String) authentication.getDetails();
        UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long senderId = userDto.getUserId();

        MessageDTO message = messageService.sendMessage(request, senderId);

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + request.getConversationId(),
                message);

        presenceService.refreshUserPresence(senderId);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingNotificationDTO notification,
            Authentication authentication) {
        if (authentication == null) {
            log.error("Authentication is null!");
            return;
        }

        String keycloakUserId = (String) authentication.getDetails();

        UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = userDto.getUserId();
        String username = userDto.getUsername();

        notification.setUserId(userId);
        notification.setUsername(username);
        notification.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + notification.getConversationId() + "/typing",
                notification);

    }

    // danh dau tin nhan da doc
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload ReadMessageRequest request,
            Authentication authentication) {
        String keycloakUserId = (String) authentication.getDetails();

        UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = userDto.getUserId();
        String username = userDto.getUsername();

        messageService.markAsRead(request.getMessageId(), userId);

        ReadReceiptDTO receipt = ReadReceiptDTO.builder()
                .messageId(request.getMessageId())
                .userId(userId)
                .username(username)
                .build();

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + request.getConversationId() + "/read",
                receipt);
    }

    // danh dau toan bo cuoc tro chuyen da doc khi an vao app
    @MessageMapping("/chat.read-conversation")
    public void markConversationAsRead(@Payload ReadConversationRequest request,
            Authentication authentication) {
        String keycloakUserId = (String) authentication.getDetails();

        UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = userDto.getUserId();

        messageService.markConversationAsRead(request.getConversationId(), userId);

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + request.getConversationId() + "/read-all",
                new ReadAllDTO(request.getConversationId(), userId));
    }

    // bắt lỗi
    @MessageExceptionHandler
    public void handleWebSocketException(Exception e, Authentication authentication) {
        log.error("WebSocket error occurred: {}", e.getMessage(), e);

        String username = authentication != null ? authentication.getName() : null;

        WebSocketErrorResponse error = WebSocketErrorResponse.builder()
                .error("WEBSOCKET_ERROR")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        if (username != null) {
            messagingTemplate.convertAndSendToUser(username, "/queue/errors", error);
        } else {
            messagingTemplate.convertAndSend("/topic/errors", error);
        }
    }

}