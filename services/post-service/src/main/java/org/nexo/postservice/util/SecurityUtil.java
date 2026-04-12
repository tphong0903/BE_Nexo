package org.nexo.postservice.util;

import lombok.RequiredArgsConstructor;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.service.GrpcServiceImpl.client.UserGrpcClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Service
@RequiredArgsConstructor
public class SecurityUtil {

    private static final String CURRENT_USER_CACHE_KEY = SecurityUtil.class.getName() + ".CURRENT_USER";

    private final TokenService tokenService;
    private final UserGrpcClient userClient;

    public void checkOwner(Long id) {
        Long currentUserId = getUserIdFromToken();
        if (!currentUserId.equals(id)) {
            throw new CustomException("You are not Owner", HttpStatus.BAD_REQUEST);
        }
    }

    public String getKeycloakId() {
        return tokenService.getKeyloakIdFromContext();
    }

    public Long getUserIdFromToken() {
        return getCurrentUser().getUserId();
    }


    public UserServiceProto.UserDto getCurrentUser() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            Object cached = requestAttributes.getAttribute(CURRENT_USER_CACHE_KEY, RequestAttributes.SCOPE_REQUEST);
            if (cached instanceof UserServiceProto.UserDto currentUser) {
                return currentUser;
            }
        }

        String sub = tokenService.getKeyloakIdFromContext();
        UserServiceProto.UserDto currentUser = userClient.getUserByKeycloakId(sub);

        if (currentUser == null || currentUser.getUserId() <= 0) {
            throw new CustomException("Authenticated user not found", HttpStatus.UNAUTHORIZED);
        }

        if (requestAttributes != null) {
            requestAttributes.setAttribute(CURRENT_USER_CACHE_KEY, currentUser, RequestAttributes.SCOPE_REQUEST);
        }

        return currentUser;
    }


    public String getCurrentUserRole() {
        String role = getCurrentUser().getRole();
        return (role == null || role.isBlank()) ? "ROLE_USER" : role;
    }

    public boolean isPrivilegedUser() {
        String role = getCurrentUserRole();
        return "ROLE_ADMIN".equals(role) || "ROLE_MODERATOR".equals(role);
    }
}
