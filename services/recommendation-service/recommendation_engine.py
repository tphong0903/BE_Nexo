from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import numpy as np
import torch
import torch.nn.functional as F
from torch_geometric.utils import negative_sampling

from config import settings
from data_loader import FollowRecord, UserRecord, load_follows, load_users
from feature_engineering import build_graph_stats, user_feature_vector
from model import GraphSAGE
from state import following_map, redis_client, user_vector_map, vector_store


@dataclass
class GraphData:
    user_ids: list[int]
    user_id_to_idx: dict[int, int]
    x: torch.Tensor
    edge_index: torch.Tensor


class RecommendationEngine:
    def __init__(self) -> None:
        self.model = GraphSAGE(
            in_channels=settings.feature_dim,
            hidden_channels=settings.hidden_dim,
            out_channels=settings.embedding_dim,
        )
        self.graph_data: GraphData | None = None

    def _artifact_path(self, filename: str) -> Path:
        return settings.artifact_dir / filename

    def _build_graph(self, users: list[UserRecord], follows: list[FollowRecord]) -> GraphData:
        user_ids = [u.id for u in users]
        user_id_to_idx = {uid: i for i, uid in enumerate(user_ids)}

        stats = build_graph_stats(users, follows)
        features = np.vstack([user_feature_vector(u, stats) for u in users]).astype(np.float32) if users else np.empty(
            (0, settings.feature_dim), dtype=np.float32
        )
        x = torch.tensor(features, dtype=torch.float32)

        edges: list[tuple[int, int]] = []
        following_map.clear()

        for f in follows:
            if f.status and f.status.upper() != "ACTIVE":
                continue
            if f.follower_id not in user_id_to_idx or f.following_id not in user_id_to_idx:
                continue

            src = user_id_to_idx[f.follower_id]
            dst = user_id_to_idx[f.following_id]
            edges.append((src, dst))
            edges.append((dst, src))

            following_map.setdefault(f.follower_id, set()).add(f.following_id)

        if edges:
            edge_index = torch.tensor(edges, dtype=torch.long).t().contiguous()
        else:
            edge_index = torch.empty((2, 0), dtype=torch.long)

        return GraphData(user_ids=user_ids, user_id_to_idx=user_id_to_idx, x=x, edge_index=edge_index)

    def _save_artifacts(self, embeddings: np.ndarray, user_ids: list[int]) -> None:
        np.save(self._artifact_path(settings.embeddings_file), embeddings)
        np.save(self._artifact_path(settings.user_ids_file), np.array(user_ids, dtype=np.int64))
        torch.save(self.model.state_dict(), self._artifact_path(settings.model_weights_file))

    def _load_model_weights_if_exists(self) -> None:
        model_path = self._artifact_path(settings.model_weights_file)
        if model_path.exists():
            self.model.load_state_dict(torch.load(model_path, map_location="cpu"))

    def _train_link_prediction(self, graph: GraphData) -> None:
        if graph.x.size(0) == 0:
            return

        optimizer = torch.optim.Adam(self.model.parameters(), lr=settings.training_lr)
        x, edge_index = graph.x, graph.edge_index

        for _ in range(settings.training_epochs):
            self.model.train()
            z = self.model(x, edge_index)

            if edge_index.size(1) == 0:
                loss = (z * 0.0).sum()
            else:
                pos_edge = edge_index
                neg_edge = negative_sampling(
                    edge_index=edge_index,
                    num_nodes=x.size(0),
                    num_neg_samples=max(pos_edge.size(1) * settings.negative_ratio, 1),
                    method="sparse",
                )

                pos_score = (z[pos_edge[0]] * z[pos_edge[1]]).sum(dim=1)
                neg_score = (z[neg_edge[0]] * z[neg_edge[1]]).sum(dim=1)

                pos_loss = F.binary_cross_entropy_with_logits(pos_score, torch.ones_like(pos_score))
                neg_loss = F.binary_cross_entropy_with_logits(neg_score, torch.zeros_like(neg_score))
                loss = pos_loss + neg_loss

            optimizer.zero_grad()
            loss.backward()
            optimizer.step()

    def _forward_embeddings(self, graph: GraphData) -> np.ndarray:
        self.model.eval()
        with torch.no_grad():
            z = self.model(graph.x, graph.edge_index)
            z = F.normalize(z, p=2, dim=1)
        return z.cpu().numpy().astype(np.float32)

    def _apply_embeddings_to_state(self, graph: GraphData, embeddings: np.ndarray) -> None:
        user_vector_map.clear()
        for idx, uid in enumerate(graph.user_ids):
            user_vector_map[uid] = embeddings[idx]

        vector_store.rebuild(graph.user_ids, embeddings)
        self.graph_data = graph

    def retrain_full(self) -> dict:
        users = load_users()
        follows = load_follows()

        graph = self._build_graph(users, follows)
        self._train_link_prediction(graph)
        embeddings = self._forward_embeddings(graph)
        self._apply_embeddings_to_state(graph, embeddings)
        self._save_artifacts(embeddings, graph.user_ids)

        self.flush_cache()

        return {"status": "ok", "users": len(graph.user_ids), "edges": int(graph.edge_index.size(1) // 2)}

    def load_or_bootstrap(self) -> None:
        users = load_users()
        follows = load_follows()
        graph = self._build_graph(users, follows)

        self._load_model_weights_if_exists()
        embeddings_path = self._artifact_path(settings.embeddings_file)
        user_ids_path = self._artifact_path(settings.user_ids_file)

        if embeddings_path.exists() and user_ids_path.exists():
            arr = np.load(embeddings_path)
            ids = np.load(user_ids_path).astype(np.int64).tolist()
            if len(ids) == graph.x.size(0) and arr.shape[0] == len(ids) and arr.shape[1] == settings.embedding_dim:
                graph.user_ids = ids
                graph.user_id_to_idx = {uid: i for i, uid in enumerate(ids)}
                self._apply_embeddings_to_state(graph, arr.astype(np.float32))
                return

        embeddings = self._forward_embeddings(graph)
        self._apply_embeddings_to_state(graph, embeddings)

    def recommend(self, user_id: int, top_k: int) -> list[int]:
        vector = user_vector_map.get(user_id)
        if vector is None:
            return []

        exclude_ids = {user_id}
        exclude_ids.update(following_map.get(user_id, set()))
        return vector_store.search(vector, top_k, exclude_ids=exclude_ids)

    def on_follow_event(self, follower_id: int, following_id: int) -> None:
        following_map.setdefault(follower_id, set()).add(following_id)

        if self.graph_data is None:
            self.load_or_bootstrap()
            return

        graph = self.graph_data
        if follower_id not in graph.user_id_to_idx or following_id not in graph.user_id_to_idx:
            self.load_or_bootstrap()
            return

        src = graph.user_id_to_idx[follower_id]
        dst = graph.user_id_to_idx[following_id]
        new_edges = torch.tensor([[src, dst], [dst, src]], dtype=torch.long)
        graph.edge_index = torch.cat([graph.edge_index, new_edges], dim=1)

        embeddings = self._forward_embeddings(graph)
        self._apply_embeddings_to_state(graph, embeddings)

        self.evict_user_cache(follower_id)
        self.evict_user_cache(following_id)

    def evict_user_cache(self, user_id: int) -> None:
        keys = list(redis_client.scan_iter(match=f"rec:user:{user_id}:k:*", count=100))
        if keys:
            redis_client.delete(*keys)

    def flush_cache(self) -> None:
        keys = list(redis_client.scan_iter(match="rec:user:*", count=500))
        if keys:
            redis_client.delete(*keys)


engine = RecommendationEngine()
