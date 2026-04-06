from __future__ import annotations

from dataclasses import dataclass

import requests

from config import settings


@dataclass
class UserRecord:
    id: int
    username: str
    bio: str
    is_private: bool
    gender: str
    created_at: str
    last_active_at: str
    activity_score: float
    post_frequency: float
    mutual_interactions: float


@dataclass
class FollowRecord:
    follower_id: int
    following_id: int
    status: str


def _extract_response_data(payload: dict) -> list[dict]:
    data = payload.get("data", [])
    if isinstance(data, list):
        return data
    return []


def _headers() -> dict[str, str]:
    return {"X-Internal-Token": settings.internal_token}


def load_users() -> list[UserRecord]:
    url = f"{settings.user_service_base_url}/internal/recommendation/users"
    response = requests.get(url, headers=_headers(), timeout=15)
    response.raise_for_status()

    records = _extract_response_data(response.json())
    users: list[UserRecord] = []
    for item in records:
        users.append(
            UserRecord(
                id=int(item.get("id")),
                username=str(item.get("username") or ""),
                bio=str(item.get("bio") or ""),
                is_private=bool(item.get("isPrivate", False)),
                gender=str(item.get("gender") or "UNKNOWN"),
                created_at=str(item.get("createdAt") or ""),
                last_active_at=str(item.get("lastActiveAt") or item.get("lastLogin") or ""),
                activity_score=float(item.get("activityScore") or 0.0),
                post_frequency=float(item.get("postFrequency") or 0.0),
                mutual_interactions=float(item.get("mutualInteractions") or 0.0),
            )
        )
    return users


def load_follows() -> list[FollowRecord]:
    url = f"{settings.user_service_base_url}/internal/recommendation/follows"
    response = requests.get(url, headers=_headers(), timeout=15)
    response.raise_for_status()

    records = _extract_response_data(response.json())
    follows: list[FollowRecord] = []
    for item in records:
        follows.append(
            FollowRecord(
                follower_id=int(item.get("followerId")),
                following_id=int(item.get("followingId")),
                status=str(item.get("status") or "ACTIVE"),
            )
        )
    return follows
