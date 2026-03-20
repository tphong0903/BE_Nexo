from __future__ import annotations

import json
import time

from kafka import KafkaConsumer

from config import settings
from recommendation_engine import engine


def run_consumer() -> None:
    engine.load_or_bootstrap()

    consumer = KafkaConsumer(
        settings.kafka_topic_user_events,
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
        auto_offset_reset="latest",
        enable_auto_commit=True,
    )

    for msg in consumer:
        payload = msg.value or {}
        event_type = payload.get("eventType")

        try:
            if event_type == "USER_FOLLOWED":
                engine.on_follow_event(int(payload["followerId"]), int(payload["followingId"]))
            elif event_type == "RECOMMENDATION_RELOAD":
                engine.retrain_full()
        except Exception as exc:
            print(f"[consumer] failed handling {event_type}: {exc}")


if __name__ == "__main__":
    while True:
        try:
            run_consumer()
        except Exception as exc:
            print(f"[consumer] crash: {exc}")
            time.sleep(3)
