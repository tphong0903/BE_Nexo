import grpc
from concurrent import futures
import os

import moderation_pb2_grpc, moderation_pb2
from app.predictor import predict


class ModerationService(moderation_pb2_grpc.ModerationServiceServicer):

    def Predict(self, request, context):
        result = predict(request.text)

        res_label = result["label"]
        res_confidence = result["confidence"]

        return moderation_pb2.PredictionResponse(
            label=str(res_label),
            confidence=float(res_confidence)
        )


def start_grpc():

    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))

    moderation_pb2_grpc.add_ModerationServiceServicer_to_server(
        ModerationService(), server
    )

    grpc_port = int(os.getenv("GRPC_PORT", "50051"))
    server.add_insecure_port(f"[::]:{grpc_port}")

    server.start()

    print(f"gRPC server running on port {grpc_port}")

    server.wait_for_termination()