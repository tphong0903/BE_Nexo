from __future__ import annotations

import json
from typing import Any

from fastapi import FastAPI, HTTPException, Query

from config import settings
from recommendation_engine import engine
from state import redis_client

app = FastAPI(title="recommendation-service")


def _cache_key(user_id: int, top_k: int) -> str:
    return f"rec:user:{user_id}:k:{top_k}"


def _redis_get_list(key: str) -> list[int] | None:
    raw = redis_client.get(key)
    if not raw:
        return None
    try:
        arr = json.loads(raw)
        if isinstance(arr, list):
            return [int(x) for x in arr]
    except (json.JSONDecodeError, ValueError, TypeError):
        return None
    return None


def _redis_set_list(key: str, values: list[int]) -> None:
    redis_client.setex(key, settings.redis_ttl_seconds, json.dumps(values))


@app.on_event("startup")
def startup_event() -> None:
    engine.load_or_bootstrap()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/recommend/{user_id}")
def recommend_friends(user_id: int, k: int = Query(default=settings.default_top_k, ge=1, le=100)) -> dict[str, Any]:
    key = _cache_key(user_id, k)
    cached = _redis_get_list(key)
    if cached is not None:
        return {"user_id": user_id, "suggested_friend_ids": cached, "source": "redis"}

    result = engine.recommend(user_id, k)

    has_user = engine.graph_data is not None and user_id in engine.graph_data.user_id_to_idx
    if not has_user:
        raise HTTPException(status_code=404, detail=f"User {user_id} not found")

    _redis_set_list(key, result)
    return {"user_id": user_id, "suggested_friend_ids": result, "source": "graphsage+faiss"}


@app.post("/internal/reload")
def reload_data() -> dict[str, str]:
    engine.load_or_bootstrap()
    engine.flush_cache()
    return {"status": "reloaded"}
