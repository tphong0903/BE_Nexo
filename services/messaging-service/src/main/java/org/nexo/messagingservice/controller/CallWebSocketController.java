package org.nexo.messagingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.messagingservice.dto.*;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.service.CallService;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CallWebSocketController {

    private final CallService callService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserGrpcClient userGrpcClient;

    @MessageMapping("/call.initiate")
    public void initiateCall(@Payload CallInitiateRequest request,
            Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        String keycloakUserId = (String) authentication.getDetails();
        UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long callerUserId = userDto.getUserId();

        CallNotificationDTO notification = callService.initiateCall(request, callerUserId);

        Long calleeUserId = callService.getOtherParticipantId(notification.getCallId(), callerUserId);
        messagingTemplate.convertAndSendToUser(
                calleeUserId.toString(),
                "/queue/call/incoming",
                notification);

        // Send callId back to caller so they can end/cancel the call before callee responds
        messagingTemplate.convertAndSendToUser(
                callerUserId.toString(),
                "/queue/call/initiated",
                notification);

        log.info("Call {} initiated by user {} to user {} (type={})",
                notification.getCallId(), callerUserId, calleeUserId, request.getCallType());
    }

    @MessageMapping("/call.response")
    public void respondToCall(@Payload CallResponseRequest request,
            Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        String keycloakUserId = (String) authentication.getDetails();
        UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long calleeUserId = userDto.getUserId();

        CallResponseDTO response = callService.respondToCall(request, calleeUserId);

        Long callerUserId = callService.getOtherParticipantId(request.getCallId(), calleeUserId);
        messagingTemplate.convertAndSendToUser(
                callerUserId.toString(),
                "/queue/call/response",
                response);

        if (response.getCallMessage() != null && response.getConversationId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + response.getConversationId(),
                    response.getCallMessage());
        }

        log.info("Call {} response from user {}: accepted={}",
                request.getCallId(), calleeUserId, request.getAccepted());
    }

    @MessageMapping("/call.signal")
    public void signal(@Payload CallSignalRequest request,
            Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        String keycloakUserId = (String) authentication.getDetails();
        UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long senderUserId = userDto.getUserId();

        CallSignalDTO signalDTO = callService.relaySignal(request, senderUserId);

        Long receiverUserId = callService.getOtherParticipantId(request.getCallId(), senderUserId);
        messagingTemplate.convertAndSendToUser(
                receiverUserId.toString(),
                "/queue/call/signal",
                signalDTO);
    }

    @MessageMapping("/call.end")
    public void endCall(@Payload CallEndRequest request,
            Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        String keycloakUserId = (String) authentication.getDetails();
        UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
        Long userId = userDto.getUserId();

        Long otherUserId = callService.getOtherParticipantId(request.getCallId(), userId);

        CallEndedDTO endedDTO = callService.endCall(request, userId);

        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/call/ended", endedDTO);
        messagingTemplate.convertAndSendToUser(otherUserId.toString(), "/queue/call/ended", endedDTO);

        if (endedDTO.getCallMessage() != null && endedDTO.getConversationId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + endedDTO.getConversationId(),
                    endedDTO.getCallMessage());
        }

        log.info("Call {} ended by user {} (finalStatus={}, duration={}s)",
                request.getCallId(), userId, endedDTO.getFinalStatus(), endedDTO.getDurationSeconds());
    }

    @MessageExceptionHandler
    public void handleCallException(Exception e, Authentication authentication) {
        log.error("Call WebSocket error: {}", e.getMessage(), e);

        String username = authentication != null ? authentication.getName() : null;

        WebSocketErrorResponse error = WebSocketErrorResponse.builder()
                .error("CALL_ERROR")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        if (username != null) {
            messagingTemplate.convertAndSendToUser(username, "/queue/errors", error);
        }
    }
}
