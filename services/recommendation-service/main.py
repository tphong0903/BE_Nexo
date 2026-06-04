from __future__ import annotations

import json
import logging
import time
import uuid
from typing import Any

from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.responses import JSONResponse, Response
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest

from config import settings
from metrics import CACHE_COUNT, RECOMMEND_ERRORS, RECOMMEND_SOURCE_COUNT, REQUEST_COUNT, REQUEST_LATENCY_SECONDS
from recommendation_engine import engine
from state import redis_client

app = FastAPI(title="recommendation-service")
logger = logging.getLogger("recommendation")
logging.basicConfig(level=logging.INFO)


@app.middleware("http")
async def metrics_middleware(request: Request, call_next):
    request_id = str(uuid.uuid4())
    start = time.perf_counter()

    request.state.request_id = request_id
    method = request.method
    endpoint = request.url.path

    try:
        response = await call_next(request)
        status_code = str(response.status_code)
    except Exception:
        status_code = "500"
        REQUEST_COUNT.labels(method=method, endpoint=endpoint, status=status_code).inc()
        REQUEST_LATENCY_SECONDS.labels(method=method, endpoint=endpoint).observe(time.perf_counter() - start)
        raise

    duration = time.perf_counter() - start
    REQUEST_COUNT.labels(method=method, endpoint=endpoint, status=status_code).inc()
    REQUEST_LATENCY_SECONDS.labels(method=method, endpoint=endpoint).observe(duration)

    response.headers["X-Request-Id"] = request_id
    logger.info(
        "request_done request_id=%s method=%s endpoint=%s status=%s latency_ms=%.3f",
        request_id,
        method,
        endpoint,
        status_code,
        duration * 1000,
    )
    return response


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
    try:
        engine.load_or_bootstrap()
    except Exception as exc:
        logger.warning("startup bootstrap failed (will retry on next request): %s", exc)


@app.get("/health/live")
def health_live() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/health/ready")
def health_ready() -> dict[str, str]:
    if engine.graph_data is None:
        raise HTTPException(status_code=503, detail="engine not initialized")
    return {"status": "ready"}


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/metrics")
def metrics() -> Response:
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)


@app.get("/recommend/{user_id}")
def recommend_friends(request: Request, user_id: int, k: int = Query(default=settings.default_top_k, ge=1, le=100)) -> dict[str, Any]:
    request_id = getattr(request.state, "request_id", "-")
    key = _cache_key(user_id, k)
    cached = _redis_get_list(key)
    if cached is not None:
        CACHE_COUNT.labels(result="hit").inc()
        RECOMMEND_SOURCE_COUNT.labels(source="redis").inc()
        logger.info(
            "recommend request_id=%s user_id=%s source=redis k=%s result_count=%s",
            request_id,
            user_id,
            k,
            len(cached),
        )
        return {"user_id": user_id, "suggested_friend_ids": cached, "source": "redis"}

    CACHE_COUNT.labels(result="miss").inc()

    if engine.graph_data is None:
        try:
            engine.load_or_bootstrap()
        except Exception as exc:
            logger.warning("lazy bootstrap failed: %s", exc)

    result = engine.recommend(user_id, k)

    has_user = engine.graph_data is not None and user_id in engine.graph_data.user_id_to_idx
    if not has_user:
        RECOMMEND_ERRORS.labels(type="user_not_found").inc()
        raise HTTPException(status_code=404, detail=f"User {user_id} not found")

    _redis_set_list(key, result)
    RECOMMEND_SOURCE_COUNT.labels(source="graphsage_faiss").inc()
    logger.info(
        "recommend request_id=%s user_id=%s source=graphsage_faiss k=%s result_count=%s",
        request_id,
        user_id,
        k,
        len(result),
    )
    return {"user_id": user_id, "suggested_friend_ids": result, "source": "graphsage+faiss"}


@app.post("/internal/reload")
def reload_data() -> dict[str, str]:
    engine.load_or_bootstrap()
    engine.flush_cache()
    return {"status": "reloaded"}


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    RECOMMEND_ERRORS.labels(type="unhandled_exception").inc()
    request_id = getattr(request.state, "request_id", "-")
    logger.exception("unhandled request_id=%s error=%s", request_id, exc)
    return JSONResponse(status_code=500, content={"detail": "internal error", "request_id": request_id})
