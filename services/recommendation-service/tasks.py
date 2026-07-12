from __future__ import annotations

from celery_app import celery_app


@celery_app.task(name="tasks.retrain_graphsage_task")
def retrain_graphsage_task() -> dict:
    from train import retrain_graphsage_batch
    return retrain_graphsage_batch()
