from __future__ import annotations

from prometheus_client import Counter, Histogram

REQUEST_COUNT = Counter(
    "recommendation_requests_total",
    "Total HTTP requests",
    ["method", "endpoint", "status"],
)

REQUEST_LATENCY_SECONDS = Histogram(
    "recommendation_request_latency_seconds",
    "HTTP request latency in seconds",
    ["method", "endpoint"],
    buckets=(0.001, 0.005, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0),
)

RECOMMEND_SOURCE_COUNT = Counter(
    "recommendation_source_total",
    "Recommend response source",
    ["source"],
)

CACHE_COUNT = Counter(
    "recommendation_cache_total",
    "Redis cache usage",
    ["result"],
)

RECOMMEND_ERRORS = Counter(
    "recommendation_errors_total",
    "Recommendation endpoint errors",
    ["type"],
)
