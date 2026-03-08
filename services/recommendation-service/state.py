from __future__ import annotations

import numpy as np
import redis

from config import settings
from vector_store import VectorStore

redis_client = redis.Redis(
    host=settings.redis_host,
    port=settings.redis_port,
    password=settings.redis_password,
    db=settings.redis_db,
    decode_responses=True,
)

vector_store = VectorStore(settings.embedding_dim)
user_vector_map: dict[int, np.ndarray] = {}
following_map: dict[int, set[int]] = {}
