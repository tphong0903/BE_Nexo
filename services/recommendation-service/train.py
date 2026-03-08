from __future__ import annotations

from recommendation_engine import engine


def retrain_graphsage_batch() -> dict:
    return engine.retrain_full()


if __name__ == "__main__":
    print(retrain_graphsage_batch())
