import threading
import uvicorn

from app.fastapi_server import app
from app.grpc_server import start_grpc
from app.eureka_client import register_eureka


def start_fastapi():
    uvicorn.run(app, host="localhost", port=8000)


if __name__ == "__main__":

    print("Registering Eureka...")
    register_eureka()

    print("Starting gRPC server...")
    grpc_thread = threading.Thread(target=start_grpc)
    grpc_thread.start()

    print("Starting FastAPI server...")
    start_fastapi()