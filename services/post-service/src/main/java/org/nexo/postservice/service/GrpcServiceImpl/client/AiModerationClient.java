package org.nexo.postservice.service.GrpcServiceImpl.client;

import moderation.Moderation;
import moderation.ModerationServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class AiModerationClient {

    @GrpcClient("ai-service")
    private ModerationServiceGrpc.ModerationServiceBlockingStub stub;

    public Moderation.PredictionResponse checkText(String text) {

        Moderation.TextRequest request = Moderation.TextRequest.newBuilder()
                .setText(text)
                .build();

        return stub.predict(request);
    }
}