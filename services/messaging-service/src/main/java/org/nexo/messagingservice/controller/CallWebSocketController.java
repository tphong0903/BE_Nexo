package org.nexo.messagingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.messagingservice.dto.*;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.repository.CallParticipantRepository;
import org.nexo.messagingservice.repository.ConversationParticipantRepository;
import org.nexo.messagingservice.service.CallService;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CallWebSocketController {

    private final CallService callService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserGrpcClient userGrpcClient;
    private final ConversationParticipantRepository participantRepository;
    private final CallParticipantRepository callParticipantRepository;

    @MessageMapping("/call.initiate")
    public void initiateCall(@Payload CallInitiateRequest request,
            Authentication authentication) {
        if (authentication == null) throw new AccessDeniedException("Authentication is required");
        String keycloakUserId = (String) authentication.getDetails();
        Long callerUserId = userGrpcClient.getUserByKeycloakId(keycloakUserId).getUserId();

        log.info("[CALL] initiateCall: conversationId={}, callerUserId={}, callType={}",
                request.getConversationId(), callerUserId, request.getCallType());

        CallNotificationDTO notification = callService.initiateCall(request, callerUserId);

        log.info("[CALL] call created: callId={}, isGroupCall={}", notification.getCallId(), notification.isGroupCall());

        if (notification.isGroupCall()) {
            List<Long> participantIds = participantRepository.findActiveUserIdsByConversationId(request.getConversationId());
            log.info("[CALL] group call: broadcasting incoming to {} participants (excluding caller)", participantIds.size() - 1);
            for (Long userId : participantIds) {
                if (!userId.equals(callerUserId)) {
                    messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/call/incoming", notification);
                    log.debug("[CALL] sent incoming to userId={}", userId);
                }
            }
        } else {
            Long calleeUserId = callService.getOtherParticipantId(notification.getCallId(), callerUserId);
            log.info("[CALL] 1-1 call: sending incoming to calleeUserId={}", calleeUserId);
            messagingTemplate.convertAndSendToUser(calleeUserId.toString(), "/queue/call/incoming", notification);
        }

        messagingTemplate.convertAndSendToUser(callerUserId.toString(), "/queue/call/initiated", notification);
        log.info("[CALL] initiated event sent to caller userId={}", callerUserId);
    }

    @MessageMapping("/call.ping")
    public void pingUser(@Payload CallSignalRequest request, Authentication authentication) {
        if (authentication == null) throw new AccessDeniedException("Authentication is required");
        Long callerUserId = userGrpcClient.getUserByKeycloakId((String) authentication.getDetails()).getUserId();
        
        CallNotificationDTO notification = callService.pingUser(request.getCallId(), request.getTargetUserId(), callerUserId);
        messagingTemplate.convertAndSendToUser(request.getTargetUserId().toString(), "/queue/call/incoming", notification);
    }

    @MessageMapping("/call.join")
    public void joinCall(@Payload CallSignalRequest request, Authentication authentication) {
        if (authentication == null) throw new AccessDeniedException("Authentication is required");
        Long userId = userGrpcClient.getUserByKeycloakId((String) authentication.getDetails()).getUserId();

        CallResponseDTO response = callService.joinActiveCall(request.getCallId(), userId);
        
        callParticipantRepository.findByCallId(request.getCallId()).forEach(cp -> {
            messagingTemplate.convertAndSendToUser(cp.getUserId().toString(), "/queue/call/response", response);
        });
    }

    @MessageMapping("/call.response")
    public void respondToCall(@Payload CallResponseRequest request,
            Authentication authentication) {
        if (authentication == null) throw new AccessDeniedException("Authentication is required");
        Long calleeUserId = userGrpcClient.getUserByKeycloakId((String) authentication.getDetails()).getUserId();

        CallResponseDTO response = callService.respondToCall(request, calleeUserId);

        if (response.isGroupCall()) {
            callParticipantRepository.findByCallId(request.getCallId()).forEach(cp -> {
                messagingTemplate.convertAndSendToUser(cp.getUserId().toString(), "/queue/call/response", response);
            });
        } else {
            Long callerUserId = callService.getOtherParticipantId(request.getCallId(), calleeUserId);
            messagingTemplate.convertAndSendToUser(callerUserId.toString(), "/queue/call/response", response);
            messagingTemplate.convertAndSendToUser(calleeUserId.toString(), "/queue/call/response", response);
        }

        if (response.getCallMessage() != null && response.getConversationId() != null) {
            messagingTemplate.convertAndSend("/topic/conversation/" + response.getConversationId(), response.getCallMessage());
        }
    }

    @MessageMapping("/call.signal")
    public void signal(@Payload CallSignalRequest request,
            Authentication authentication) {
        if (authentication == null) throw new AccessDeniedException("Authentication is required");
        Long senderUserId = userGrpcClient.getUserByKeycloakId((String) authentication.getDetails()).getUserId();

        CallSignalDTO signalDTO = callService.relaySignal(request, senderUserId);

        Long receiverUserId = signalDTO.isGroupCall() ? request.getTargetUserId() : callService.getOtherParticipantId(request.getCallId(), senderUserId);
        messagingTemplate.convertAndSendToUser(receiverUserId.toString(), "/queue/call/signal", signalDTO);
    }

    @MessageMapping("/call.end")
    public void endCall(@Payload CallEndRequest request,
            Authentication authentication) {
        if (authentication == null) throw new AccessDeniedException("Authentication is required");
        Long userId = userGrpcClient.getUserByKeycloakId((String) authentication.getDetails()).getUserId();

        CallEndedDTO endedDTO = callService.endCall(request, userId);

        callParticipantRepository.findByCallId(request.getCallId()).forEach(cp -> {
            messagingTemplate.convertAndSendToUser(cp.getUserId().toString(), "/queue/call/ended", endedDTO);
        });

        if (endedDTO.getCallMessage() != null && endedDTO.getConversationId() != null) {
            messagingTemplate.convertAndSend("/topic/conversation/" + endedDTO.getConversationId(), endedDTO.getCallMessage());
        }
    }

    @MessageExceptionHandler
    public void handleCallException(Exception e, Authentication authentication) {
        log.error("Call WebSocket error: {}", e.getMessage(), e);
        if (authentication != null) {
            WebSocketErrorResponse error = WebSocketErrorResponse.builder()
                    .error("CALL_ERROR")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            messagingTemplate.convertAndSendToUser(authentication.getName(), "/queue/errors", error);
        }
    }
}
