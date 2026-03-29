import grpc
from concurrent import futures

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

    server.add_insecure_port('[::]:50051')

    server.start()

    print("gRPC server running on port 50051")

    server.wait_for_termination()