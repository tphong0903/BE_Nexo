package org.nexo.messagingservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.nexo.grpc.user.UserServiceProto;
import org.nexo.messagingservice.dto.PresenceUpdateDTO;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.service.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;
    private final UserGrpcClient userGrpcClient;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
            String keycloakUserId = (String) auth.getDetails();

            UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
            Long userId = userDto.getUserId();
            log.info("User connected: {} (ID: {})", userId);

            presenceService.setUserOnline(userId);

            broadcastPresenceToMutualFriends(userId, true);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
            String keycloakUserId = (String) auth.getDetails();

            UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
            Long userId = userDto.getUserId();

            log.info("User disconnected: {} (ID: {})", userId);

            presenceService.setUserOffline(userId);

            broadcastPresenceToMutualFriends(userId, false);
        }
    }

    private void broadcastPresenceToMutualFriends(Long userId, boolean isOnline) {
        List<Long> mutualFriends = presenceService.getOnlineMutualFriends(userId);

        PresenceUpdateDTO presenceUpdate = PresenceUpdateDTO.builder()
                .userId(userId)
                .isOnline(isOnline)
                .timestamp(LocalDateTime.now())
                .build();

        for (Long friendId : mutualFriends) {
            messagingTemplate.convertAndSendToUser(
                    friendId.toString(),
                    "/queue/presence",
                    presenceUpdate);
        }
    }

}