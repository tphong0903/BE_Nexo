package org.nexo.messagingservice.grpc;

import org.nexo.grpc.messaging.BlockStatusChangeRequest;
import org.nexo.grpc.messaging.BlockStatusChangeResponse;
import org.nexo.grpc.messaging.MessagingServiceGrpc;
import org.nexo.messagingservice.service.Impl.ConversationService;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class MessagingGrpcServer extends MessagingServiceGrpc.MessagingServiceImplBase {

    private final ConversationService conversationService;

    @Override
    public void handleBlockStatusChange(BlockStatusChangeRequest request,
            StreamObserver<BlockStatusChangeResponse> responseObserver) {
        conversationService.handleBlockStatusChange(
                request.getUserId1(),
                request.getUserId2(),
                request.getIsBlocked());

        BlockStatusChangeResponse response = BlockStatusChangeResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Block status updated successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
