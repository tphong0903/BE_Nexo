package org.nexo.userservice.grpc;

import org.nexo.grpc.messaging.BlockStatusChangeRequest;
import org.nexo.grpc.messaging.BlockStatusChangeResponse;
import org.nexo.grpc.messaging.MessagingServiceGrpc;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;

@Slf4j
@Service
public class MessagingGrpcClient {

    @GrpcClient("messaging-service")
    private MessagingServiceGrpc.MessagingServiceBlockingStub messagingServiceStub;

    public void handleBlockStatusChange(Long userId1, Long userId2, boolean isBlocked) {
        BlockStatusChangeRequest request = BlockStatusChangeRequest.newBuilder()
                .setUserId1(userId1)
                .setUserId2(userId2)
                .setIsBlocked(isBlocked)
                .build();

        BlockStatusChangeResponse response = messagingServiceStub.handleBlockStatusChange(request);

        if (response.getSuccess()) {
            log.info("Block status change successful: {}", response.getMessage());
        } else {
            log.error("Block status change failed: {}", response.getMessage());
        }
    }
}
