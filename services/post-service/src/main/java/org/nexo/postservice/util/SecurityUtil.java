package org.nexo.postservice.util;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.service.GrpcServiceImpl.client.UserGrpcClient;
import org.nexo.postservice.util.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityUtil {
    private final TokenService tokenService;
    private final UserGrpcClient userClient;

    public void checkOwner(Long id) {
        String sub = tokenService.getKeyloakIdFromContext();
        UserServiceProto.UserDto user = userClient.getUserByKeycloakId(sub);

        if (user.getUserId() != id)
            throw new CustomException("You are not Owner", HttpStatus.BAD_REQUEST);
    }

    public String getKeyloakId() {
        return tokenService.getKeyloakIdFromContext();
    }

    public Long getUserIdFromToken() {
        String sub = tokenService.getKeyloakIdFromContext();
        UserServiceProto.UserDto user = userClient.getUserByKeycloakId(sub);
        return user.getUserId();
    }
}
