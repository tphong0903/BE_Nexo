package org.nexo.notificationservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String token = accessor.getFirstNativeHeader("Authorization");
            if (token == null) {
                token = accessor.getFirstNativeHeader("access_token");
            }

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    String username = jwt.getClaimAsString("preferred_username");

                    UsernamePasswordAuthenticationToken principal =
                            new UsernamePasswordAuthenticationToken(username, null, List.of());

                    accessor.setUser(principal);
                    log.info("[WebSocket] Principal set successfully for user: {}", username);
                } catch (Exception e) {
                    log.error("[WebSocket] Invalid token: {}", e.getMessage());
                    throw new IllegalArgumentException("Invalid JWT Token");
                }
            }
        }

        return message;
    }
}