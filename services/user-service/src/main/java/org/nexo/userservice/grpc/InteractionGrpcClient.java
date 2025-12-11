package org.nexo.userservice.grpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.grpc.interaction.InteractionServiceGrpc;
import org.nexo.grpc.interaction.InteractionServiceOuterClass;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionGrpcClient {

    @GrpcClient("interaction-service")
    private InteractionServiceGrpc.InteractionServiceBlockingStub interactionServiceStub;

    public Long getUserInteractionsCount(Long userId) {
        try {
            InteractionServiceOuterClass.GetUserInteractionsCountRequest request = InteractionServiceOuterClass.GetUserInteractionsCountRequest
                    .newBuilder()
                    .setUserId(userId)
                    .build();

            InteractionServiceOuterClass.GetUserInteractionsCountResponse response = interactionServiceStub
                    .getUserInteractionsCount(request);

            return response.getInteractionsCount();
        } catch (Exception e) {
            log.error("Error getting interactions count for userId: {}, error: {}", userId, e.getMessage());
            return 0L;
        }
    }
}
