package org.nexo.feedservice.util;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.exception.CustomException;
import org.nexo.feedservice.service.UserGrpcClient;
import org.nexo.grpc.user.UserServiceProto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SecurityUtil {
    private final TokenService tokenService;
    private final UserGrpcClient userClient;

    public Mono<Void> checkOwner(Long id) {
        return tokenService.getKeycloakIdFromContext()
                .flatMap(sub -> {
                    UserServiceProto.UserDto user = userClient.getUserByKeycloakId(sub);
                    if (user.getUserId() != id) {
                        return Mono.error(new CustomException("You are not Owner", HttpStatus.BAD_REQUEST));
                    }
                    return Mono.empty();
                });
    }

    public Mono<String> getKeycloakId() {
        return tokenService.getKeycloakIdFromContext();
    }

    public Mono<Long> getUserIdFromToken() {
        return tokenService.getKeycloakIdFromContext()
                .map(sub -> userClient.getUserByKeycloakId(sub).getUserId());
    }
}
