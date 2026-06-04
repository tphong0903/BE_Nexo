package org.nexo.postservice.service.GrpcServiceImpl.client;

import net.devh.boot.grpc.client.inject.GrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.nexo.grpc.user.UserServiceGrpc;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.postservice.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
public class UserGrpcClient {

    private static final long USER_GRPC_TIMEOUT_SECONDS = 3L;

    @GrpcClient("users")
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    public UserServiceProto.UserDto getUserByKeycloakId(String keycloakId) {
        try {
            UserServiceProto.KeycloakId request = UserServiceProto.KeycloakId.newBuilder()
                    .setKeycloakUserId(keycloakId)
                    .build();

            return userStub().getUserDto(request);
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException("fetch current user", ex);
        }
    }

    public UserServiceProto.GetUserFolloweesResponse getUserFollowees(Long userId) {
        try {
            UserServiceProto.GetUserFolloweesRequest request = UserServiceProto.GetUserFolloweesRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            return userStub().getUserFollowees(request);
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException("fetch user followees", ex);
        }
    }

//    public UserServiceProto.GetUserFollowingsResponse getUserFollowing(Long userId) {
//        UserServiceProto.GetUserFollowingsRequest request = UserServiceProto.GetUserFollowingsRequest.newBuilder()
//                .setUserId(userId)
//                .build();
//
//        return userStub.getUserFollowings(request);
//    }

    public UserServiceProto.UserDTOResponse getUserDTOById(Long userId) {
        try {
            UserServiceProto.GetUserDtoByIdRequest request = UserServiceProto.GetUserDtoByIdRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            return userStub().getUserDtoById(request);
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException("fetch user by id", ex);
        }
    }

    public UserServiceProto.CheckFollowResponse checkFollow(Long userId, Long userId2) {
        try {
            UserServiceProto.CheckMutualFollowRequest request = UserServiceProto.CheckMutualFollowRequest.newBuilder()
                    .setUserId1(userId)
                    .setUserId2(userId2)
                    .build();

            return userStub().checkFollow(request);
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException("check follow status", ex);
        }
    }

    public List<UserServiceProto.UserDTOResponse2> getUsersByIds(List<Long> tagIds) {
        try {
            UserServiceProto.GetUsersByIdsRequest request = UserServiceProto.GetUsersByIdsRequest.newBuilder()
                    .addAllUserIds(tagIds)
                    .build();

            UserServiceProto.GetUsersByIdsResponse response = userStub().getUsersByIds(request);
            return response.getUsersList();
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException("fetch users by ids", ex);
        }
    }

    public Long getTotalUsers() {
        try {
            UserServiceProto.QuantityTotalUsers response = userStub()
                    .getTotalUsers(UserServiceProto.Empty.newBuilder().build());
            return response.getQuantity();
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException("fetch total users", ex);
        }
    }

    public Double getPercentUsersInThisMonth() {
        try {
            UserServiceProto.UserPercentResponse response = userStub()
                    .getPercentUsersInThisMonth(UserServiceProto.Empty.newBuilder().build());
            return response.getPercent();
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException("fetch monthly user percentage", ex);
        }
    }

    public List<UserServiceProto.UserCountByDate> getUsersByTime(LocalDate startDate, LocalDate endDate) {
        try {
            UserServiceProto.DateRange request = UserServiceProto.DateRange.newBuilder()
                    .setStartDate(startDate.toString())
                    .setEndDate(endDate.toString())
                    .build();

            UserServiceProto.GetUsersByTimeResponse response = userStub().getUsersByTime(request);
            return response.getDataList();
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException("fetch users by time", ex);
        }
    }

    public UserServiceProto.GetUserFollowingsResponse getUserFollowing(Long userId) {
        try {
            UserServiceProto.GetUserFollowingsRequest request = UserServiceProto.GetUserFollowingsRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            return userStub().getUserFollowings(request);
        } catch (StatusRuntimeException ex) {
            throw mapGrpcException("fetch user followings", ex);
        }
    }

    private UserServiceGrpc.UserServiceBlockingStub userStub() {
        return userStub.withDeadlineAfter(USER_GRPC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private CustomException mapGrpcException(String action, StatusRuntimeException ex) {
        Status.Code code = ex.getStatus().getCode();
        HttpStatus status = switch (code) {
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_GATEWAY;
        };

        return new CustomException("User service failed while trying to " + action, status);
    }

}
