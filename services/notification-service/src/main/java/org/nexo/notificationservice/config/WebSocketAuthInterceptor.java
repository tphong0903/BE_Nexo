package org.nexo.notificationservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        String token = accessor.getFirstNativeHeader("Authorization");
        if (token == null) {
            List<String> tokens = accessor.getNativeHeader("access_token");
            if (tokens != null && !tokens.isEmpty()) token = tokens.get(0);
        }

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                Jwt jwt = jwtDecoder.decode(token);
                String username = jwt.getClaimAsString("preferred_username");
                UsernamePasswordAuthenticationToken principal =
                        new UsernamePasswordAuthenticationToken(username, null, List.of());
                accessor.setUser(principal);
                log.info("[WebSocket] Principal set: {}", username);
            } catch (Exception e) {
                log.error("[WebSocket] Invalid token: {}", e.getMessage());
            }
        }

        return message;
    }

}
