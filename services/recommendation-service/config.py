import os
from pathlib import Path


class Settings:
    service_port: int = int(os.getenv("SERVICE_PORT", "8000"))

    redis_host: str = os.getenv("REDIS_HOST", "redis")
    redis_port: int = int(os.getenv("REDIS_PORT", "6379"))
    redis_password: str | None = os.getenv("REDIS_PASSWORD", "") or None
    redis_db: int = int(os.getenv("REDIS_DB", "0"))
    redis_ttl_seconds: int = int(os.getenv("REDIS_TTL_SECONDS", "3600"))

    user_service_base_url: str = os.getenv("USER_SERVICE_BASE_URL", "http://user-service:8085")
    internal_token: str = os.getenv("INTERNAL_TOKEN", "")

    kafka_bootstrap_servers: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9097")
    kafka_topic_user_events: str = os.getenv("KAFKA_TOPIC_USER_EVENTS", "user_events")
    kafka_group_id: str = os.getenv("KAFKA_GROUP_ID", "recommendation-service-group")

    default_top_k: int = int(os.getenv("DEFAULT_TOP_K", "10"))
    feature_dim: int = int(os.getenv("FEATURE_DIM", "16"))
    hidden_dim: int = int(os.getenv("HIDDEN_DIM", "64"))
    embedding_dim: int = int(os.getenv("EMBEDDING_DIM", "32"))

    training_epochs: int = int(os.getenv("TRAINING_EPOCHS", "60"))
    training_lr: float = float(os.getenv("TRAINING_LR", "0.01"))
    negative_ratio: int = int(os.getenv("NEGATIVE_RATIO", "1"))

    celery_broker_url: str = os.getenv("CELERY_BROKER_URL", "redis://:Admin@123@redis:6379/1")
    celery_result_backend: str = os.getenv("CELERY_RESULT_BACKEND", "redis://:Admin@123@redis:6379/2")

    artifact_dir: Path = Path(os.getenv("ARTIFACT_DIR", str(Path(__file__).resolve().parent / "artifacts")))
    model_weights_file: str = os.getenv("MODEL_WEIGHTS_FILE", "model_weights.pth")
    embeddings_file: str = os.getenv("EMBEDDINGS_FILE", "embeddings.npy")
    user_ids_file: str = os.getenv("USER_IDS_FILE", "user_ids.npy")


settings = Settings()
settings.artifact_dir.mkdir(parents=True, exist_ok=True)
