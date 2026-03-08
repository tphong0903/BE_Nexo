import py_eureka_client.eureka_client as eureka_client

def register_eureka():

    eureka_client.init(
        eureka_server="http://localhost:8761/eureka",
        app_name="ai-service",
        instance_port=8000,
        instance_host="localhost"
    )