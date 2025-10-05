package org.nexo.apigateway.config;

import org.nexo.apigateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class UserKeyResolver2 {
    @Autowired
    private JwtUtil jwtUtil;

    KeyResolver userKeyResolver(JwtUtil jwtUtil) {
        return exchange -> {
            List<String> authHeaders = exchange.getRequest()
                    .getHeaders()
                    .getOrEmpty(HttpHeaders.AUTHORIZATION);

            if (authHeaders.isEmpty()) {
                return Mono.just("anonymous");
            }

            String token = authHeaders.getFirst().replace("Bearer ", "");

            String userId = jwtUtil.getUserIdFromToken(token);

            if (userId == null || userId.isEmpty()) {
                return Mono.just("anonymous");
            }

            return Mono.just(userId);
        };
    }
}
