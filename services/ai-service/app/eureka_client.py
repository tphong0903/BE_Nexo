import py_eureka_client.eureka_client as eureka_client
import os

def register_eureka():

    eureka_server = os.getenv("EUREKA_SERVER_URL", "http://eureka-server:8761/eureka")
    app_name = os.getenv("EUREKA_APP_NAME", "ai-service")
    instance_port = int(os.getenv("GRPC_PORT", "50051"))
    instance_host = os.getenv("EUREKA_INSTANCE_HOST", "ai-service")

    eureka_client.init(
        eureka_server=eureka_server,
        app_name=app_name,
        instance_port=instance_port,
        instance_host=instance_host
    )