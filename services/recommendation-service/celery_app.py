from __future__ import annotations

from celery import Celery
from celery.schedules import crontab

from config import settings

celery_app = Celery(
    "recommendation-service",
    broker=settings.celery_broker_url,
    backend=settings.celery_result_backend,
    include=["tasks"],
)

celery_app.conf.update(
    timezone="Asia/Ho_Chi_Minh",
    enable_utc=False,
    beat_schedule={
        "retrain-graphsage-2am": {
            "task": "tasks.retrain_graphsage_task",
            "schedule": crontab(hour=2, minute=0),
        }
    },
)

