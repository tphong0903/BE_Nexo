package org.nexo.messagingservice.config;

import lombok.RequiredArgsConstructor;

import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.util.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final JwtDecoder jwtDecoder;
    private final UserGrpcClient userGrpcClient;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("https://nexo.nayamishop.id.vn", "http://localhost:3000")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");

                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                        jwtDecoder.decode(token);

                        String keycloakUserId = jwtUtil.getUserIdFromToken(token);

                        if (keycloakUserId != null) {
                            try {
                                org.nexo.grpc.user.UserServiceProto.UserDto userDto = userGrpcClient.getUserByKeycloakId(keycloakUserId);
                                Long userId = userDto.getUserId();
                                
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                        userId.toString(),
                                        null,
                                        Collections.emptyList());
                                authentication.setDetails(keycloakUserId);

                                accessor.setUser(authentication);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    // Propagate authentication for SEND messages
                    if (accessor.getUser() != null) {
                        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor
                                .getUser();
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }

                return message;
            }
        });
    }
}