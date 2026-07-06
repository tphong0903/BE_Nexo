import threading
import uvicorn
import os

from app.fastapi_server import app
from app.grpc_server import start_grpc
from app.eureka_client import register_eureka


def start_fastapi():
    host = os.getenv("FASTAPI_HOST", "0.0.0.0")
    port = int(os.getenv("FASTAPI_PORT", "8000"))
    uvicorn.run(app, host=host, port=port)



if __name__ == "__main__":

    print("Registering Eureka...")
    register_eureka()

    print("Starting gRPC server...")
    grpc_thread = threading.Thread(target=start_grpc)
    grpc_thread.start()

    print("Starting FastAPI server...")
    start_fastapi()