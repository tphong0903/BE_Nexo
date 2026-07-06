from __future__ import annotations

import math
from dataclasses import dataclass
from datetime import datetime, timezone

import numpy as np

from config import settings
from data_loader import FollowRecord, UserRecord

try:
    from sentence_transformers import SentenceTransformer
except Exception:  # pragma: no cover
    SentenceTransformer = None


@dataclass
class GraphStats:
    degree: dict[int, int]
    in_degree: dict[int, int]
    out_degree: dict[int, int]
    neighbors: dict[int, set[int]]
    adamic_norm: dict[int, float]


_embedder = None


def _get_embedder():
    global _embedder
    if _embedder is not None:
        return _embedder
    if SentenceTransformer is None:
        return None
    try:
        _embedder = SentenceTransformer(settings.text_embedding_model)
        return _embedder
    except Exception:
        return None


def _safe_parse_datetime(v: str | None) -> datetime | None:
    if not v:
        return None
    try:
        text = v.replace("Z", "+00:00")
        dt = datetime.fromisoformat(text)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt
    except Exception:
        return None


def build_graph_stats(users: list[UserRecord], follows: list[FollowRecord]) -> GraphStats:
    neighbors: dict[int, set[int]] = {u.id: set() for u in users}
    in_degree: dict[int, int] = {u.id: 0 for u in users}
    out_degree: dict[int, int] = {u.id: 0 for u in users}

    for f in follows:
        if (f.status or "ACTIVE").upper() != "ACTIVE":
            continue
        if f.follower_id not in neighbors or f.following_id not in neighbors:
            continue

        out_degree[f.follower_id] += 1
        in_degree[f.following_id] += 1
        neighbors[f.follower_id].add(f.following_id)
        neighbors[f.following_id].add(f.follower_id)

    degree = {uid: in_degree[uid] + out_degree[uid] for uid in neighbors.keys()}
    adamic_norm = {
        uid: (1.0 / math.log(max(2, len(neighbors[uid])))) if len(neighbors[uid]) > 0 else 0.0
        for uid in neighbors.keys()
    }

    return GraphStats(
        degree=degree,
        in_degree=in_degree,
        out_degree=out_degree,
        neighbors=neighbors,
        adamic_norm=adamic_norm,
    )


def _normalize_numeric(values: list[float]) -> dict[int, float]:
    if not values:
        return {}
    arr = np.array(values, dtype=np.float32)
    mean = float(arr.mean())
    std = float(arr.std())
    std = std if std > 1e-8 else 1.0
    norm = (arr - mean) / std
    norm = np.tanh(norm)
    return {i: float(norm[i]) for i in range(len(values))}


def _bio_embedding_vector(bio: str, dim: int = 16) -> np.ndarray:
    model = _get_embedder()
    if model is not None:
        try:
            vec = np.array(model.encode([bio or ""])[0], dtype=np.float32)
            if vec.shape[0] >= dim:
                return vec[:dim]
            out = np.zeros((dim,), dtype=np.float32)
            out[: vec.shape[0]] = vec
            return out
        except Exception:
            pass

    # fallback deterministic hashing embedding
    seed = hash((bio or "")[:256]) & 0xFFFFFFFF
    rng = np.random.default_rng(seed)
    vec = rng.normal(0, 1, dim).astype(np.float32)
    return vec / (np.linalg.norm(vec) + 1e-8)


def user_feature_vector(user: UserRecord, stats: GraphStats) -> np.ndarray:
    now = datetime.now(timezone.utc)
    created_at = _safe_parse_datetime(user.created_at)
    last_active_at = _safe_parse_datetime(user.last_active_at)

    account_age_days = 0.0
    if created_at is not None:
        account_age_days = max((now - created_at).total_seconds() / 86400.0, 0.0)

    recency_days = 365.0
    if last_active_at is not None:
        recency_days = max((now - last_active_at).total_seconds() / 86400.0, 0.0)

    uid = user.id
    degree = float(stats.degree.get(uid, 0))
    in_degree = float(stats.in_degree.get(uid, 0))
    out_degree = float(stats.out_degree.get(uid, 0))

    activity_score = float(user.activity_score or 0.0)
    post_frequency = float(user.post_frequency or 0.0)
    mutual_interactions = float(user.mutual_interactions or 0.0)
    recency_weight = 1.0 / (1.0 + recency_days)

    common_neighbors_avg = 0.0
    jaccard_avg = 0.0
    adamic_avg = 0.0

    neighbors_u = stats.neighbors.get(uid, set())
    if neighbors_u:
        common_vals = []
        jacc_vals = []
        adamic_vals = []

        for v in neighbors_u:
            neighbors_v = stats.neighbors.get(v, set())
            inter = neighbors_u.intersection(neighbors_v)
            union = neighbors_u.union(neighbors_v)
            common = len(inter)
            jacc = (common / len(union)) if union else 0.0
            adamic = sum(stats.adamic_norm.get(w, 0.0) for w in inter)

            common_vals.append(float(common))
            jacc_vals.append(float(jacc))
            adamic_vals.append(float(adamic))

        common_neighbors_avg = float(np.mean(common_vals)) if common_vals else 0.0
        jaccard_avg = float(np.mean(jacc_vals)) if jacc_vals else 0.0
        adamic_avg = float(np.mean(adamic_vals)) if adamic_vals else 0.0

    is_private = 1.0 if user.is_private else 0.0
    is_male = 1.0 if (user.gender or "").upper() == "MALE" else 0.0
    is_female = 1.0 if (user.gender or "").upper() == "FEMALE" else 0.0

    dense = np.array(
        [
            account_age_days,
            activity_score,
            post_frequency,
            mutual_interactions,
            recency_weight,
            degree,
            in_degree,
            out_degree,
            common_neighbors_avg,
            jaccard_avg,
            adamic_avg,
            is_private,
            is_male,
            is_female,
            min(len(user.bio or "") / 500.0, 1.0),
            1.0,
        ],
        dtype=np.float32,
    )

    text_vec = _bio_embedding_vector(user.bio or "", dim=48)
    vec = np.concatenate([dense, text_vec], axis=0).astype(np.float32)
    vec = vec / (np.linalg.norm(vec) + 1e-8)
    return vec
