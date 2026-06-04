from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import numpy as np
import torch
import torch.nn.functional as F
from torch_geometric.utils import negative_sampling

from config import settings
from data_loader import BlockRecord, FollowRecord, UserRecord, load_blocks, load_follows, load_users
from feature_engineering import build_graph_stats, user_feature_vector
from model import GraphSAGE
from state import block_map, following_map, redis_client, user_vector_map, vector_store


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

    def _build_follow_edges(self, follows: list[FollowRecord], user_id_to_idx: dict[int, int]) -> torch.Tensor:
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
            return torch.tensor(edges, dtype=torch.long).t().contiguous()
        return torch.empty((2, 0), dtype=torch.long)

    def _build_graph(self, users: list[UserRecord], follows: list[FollowRecord]) -> GraphData:
        users = [u for u in users if u.account_status == "ACTIVE"]
        user_ids = [u.id for u in users]
        user_id_to_idx = {uid: i for i, uid in enumerate(user_ids)}

        stats = build_graph_stats(users, follows)
        features = np.vstack([user_feature_vector(u, stats) for u in users]).astype(np.float32) if users else np.empty(
            (0, settings.feature_dim), dtype=np.float32
        )
        x = torch.tensor(features, dtype=torch.float32)
        edge_index = self._build_follow_edges(follows, user_id_to_idx)

        return GraphData(user_ids=user_ids, user_id_to_idx=user_id_to_idx, x=x, edge_index=edge_index)

    def _save_artifacts(self, embeddings: np.ndarray, user_ids: list[int]) -> None:
        np.save(self._artifact_path(settings.embeddings_file), embeddings)
        np.save(self._artifact_path(settings.user_ids_file), np.array(user_ids, dtype=np.int64))
        torch.save(self.model.state_dict(), self._artifact_path(settings.model_weights_file))

    def _load_model_weights_if_exists(self) -> None:
        model_path = self._artifact_path(settings.model_weights_file)
        if model_path.exists():
            self.model.load_state_dict(torch.load(model_path, map_location="cpu"))

    def _load_block_map(self, blocks: list[BlockRecord]) -> None:
        block_map.clear()
        for b in blocks:
            block_map.setdefault(b.blocker_id, set()).add(b.blocked_id)

    def _build_neighbor_map(self, edge_index: torch.Tensor) -> dict[int, set[int]]:
        neighbors: dict[int, set[int]] = {}
        for src, dst in zip(edge_index[0].tolist(), edge_index[1].tolist()):
            neighbors.setdefault(src, set()).add(dst)
        return neighbors

    def _sample_hard_negatives(self, edge_index: torch.Tensor, num_nodes: int, num_samples: int) -> torch.Tensor:
        """50% hard negatives (2-hop neighbors not yet connected) + 50% random negatives."""
        rand_neg = negative_sampling(edge_index, num_nodes, num_samples, method="sparse")
        if edge_index.size(1) == 0:
            return rand_neg

        neighbors = self._build_neighbor_map(edge_index)
        pos_set = set(zip(edge_index[0].tolist(), edge_index[1].tolist()))
        node_list = list(neighbors.keys())

        hard_negs: list[tuple[int, int]] = []
        hard_target = num_samples // 2
        rng = np.random.default_rng()

        for _ in range(hard_target * 20):
            if len(hard_negs) >= hard_target:
                break
            u = int(rng.choice(node_list))
            two_hop = set()
            for v in neighbors.get(u, set()):
                two_hop.update(neighbors.get(v, set()))
            two_hop.discard(u)
            two_hop -= neighbors.get(u, set())
            if not two_hop:
                continue
            w = int(rng.choice(list(two_hop)))
            if (u, w) not in pos_set:
                hard_negs.append((u, w))

        if not hard_negs:
            return rand_neg

        hard_tensor = torch.tensor(hard_negs, dtype=torch.long).t().contiguous()
        # Fill the other half with random negatives
        fill = negative_sampling(edge_index, num_nodes, num_samples - len(hard_negs), method="sparse")
        return torch.cat([hard_tensor, fill], dim=1)

    def _train_link_prediction(self, graph: GraphData) -> None:
        if graph.x.size(0) == 0:
            return

        optimizer = torch.optim.Adam(self.model.parameters(), lr=settings.training_lr)
        x, edge_index = graph.x, graph.edge_index
        log_every = max(settings.training_epochs // 5, 1)

        for epoch in range(1, settings.training_epochs + 1):
            self.model.train()
            z = self.model(x, edge_index)

            if edge_index.size(1) == 0:
                loss = (z * 0.0).sum()
            else:
                pos_edge = edge_index
                num_neg = max(pos_edge.size(1) * settings.negative_ratio, 1)
                neg_edge = self._sample_hard_negatives(edge_index, x.size(0), num_neg)

                # Normalize during training to match inference metric (cosine via IndexFlatIP)
                z_norm = F.normalize(z, p=2, dim=1)
                pos_score = (z_norm[pos_edge[0]] * z_norm[pos_edge[1]]).sum(dim=1)
                neg_score = (z_norm[neg_edge[0]] * z_norm[neg_edge[1]]).sum(dim=1)

                pos_loss = F.binary_cross_entropy_with_logits(pos_score, torch.ones_like(pos_score))
                neg_loss = F.binary_cross_entropy_with_logits(neg_score, torch.zeros_like(neg_score))
                loss = pos_loss + neg_loss

            optimizer.zero_grad()
            loss.backward()
            optimizer.step()

            if epoch % log_every == 0 or epoch == settings.training_epochs:
                metrics = self._eval_metrics(z_norm if edge_index.size(1) > 0 else None, edge_index)
                print(
                    f"[train] epoch={epoch}/{settings.training_epochs}"
                    f" loss={loss.item():.4f}"
                    f" auc={metrics['auc']:.3f}"
                    f" ap={metrics['ap']:.3f}"
                )

    @staticmethod
    def _eval_metrics(z_norm: torch.Tensor | None, edge_index: torch.Tensor) -> dict[str, float]:
        """Compute AUC and AP on a small random sample of pos/neg edges (no grad)."""
        if z_norm is None or edge_index.size(1) == 0:
            return {"auc": 0.0, "ap": 0.0}

        try:
            from sklearn.metrics import roc_auc_score, average_precision_score
        except ImportError:
            return {"auc": 0.0, "ap": 0.0}

        with torch.no_grad():
            sample = min(edge_index.size(1), 512)
            idx = torch.randperm(edge_index.size(1))[:sample]
            pos_edge = edge_index[:, idx]

            neg_edge = negative_sampling(edge_index, z_norm.size(0), sample, method="sparse")

            pos_scores = torch.sigmoid((z_norm[pos_edge[0]] * z_norm[pos_edge[1]]).sum(dim=1)).cpu().numpy()
            neg_scores = torch.sigmoid((z_norm[neg_edge[0]] * z_norm[neg_edge[1]]).sum(dim=1)).cpu().numpy()

        y_true = np.concatenate([np.ones(len(pos_scores)), np.zeros(len(neg_scores))])
        y_score = np.concatenate([pos_scores, neg_scores])

        try:
            auc = float(roc_auc_score(y_true, y_score))
            ap = float(average_precision_score(y_true, y_score))
        except Exception:
            auc, ap = 0.0, 0.0

        return {"auc": auc, "ap": ap}

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
        blocks = load_blocks()

        self._load_block_map(blocks)
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
        blocks = load_blocks()
        self._load_block_map(blocks)

        user_ids = [u.id for u in users]
        user_id_to_idx = {uid: i for i, uid in enumerate(user_ids)}
        edge_index = self._build_follow_edges(follows, user_id_to_idx)

        self._load_model_weights_if_exists()
        embeddings_path = self._artifact_path(settings.embeddings_file)
        user_ids_path = self._artifact_path(settings.user_ids_file)

        if embeddings_path.exists() and user_ids_path.exists():
            arr = np.load(embeddings_path)
            ids = np.load(user_ids_path).astype(np.int64).tolist()
            if arr.shape[0] == len(ids) and arr.shape[1] == settings.embedding_dim:
                # Fast path: skip feature computation and sentence transformer entirely
                # New users since last retrain won't have embeddings until next retrain
                graph = GraphData(
                    user_ids=ids,
                    user_id_to_idx={uid: i for i, uid in enumerate(ids)},
                    x=torch.empty((len(ids), settings.feature_dim), dtype=torch.float32),
                    edge_index=edge_index,
                )
                self._apply_embeddings_to_state(graph, arr.astype(np.float32))
                return

        # No saved artifacts: full feature computation + GNN forward pass
        graph = self._build_graph(users, follows)
        embeddings = self._forward_embeddings(graph)
        self._apply_embeddings_to_state(graph, embeddings)

    def recommend(self, user_id: int, top_k: int) -> list[int]:
        vector = user_vector_map.get(user_id)
        if vector is None:
            return []

        exclude_ids = {user_id}
        exclude_ids.update(following_map.get(user_id, set()))
        # Loại người mà user đã block
        exclude_ids.update(block_map.get(user_id, set()))
        # Loại người đã block user (2 chiều)
        for blocker, blocked_set in block_map.items():
            if user_id in blocked_set:
                exclude_ids.add(blocker)
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

    def on_block_event(self, blocker_id: int, blocked_id: int) -> None:
        block_map.setdefault(blocker_id, set()).add(blocked_id)
        self.evict_user_cache(blocker_id)
        self.evict_user_cache(blocked_id)

    def on_unblock_event(self, blocker_id: int, blocked_id: int) -> None:
        block_map.get(blocker_id, set()).discard(blocked_id)
        self.evict_user_cache(blocker_id)
        self.evict_user_cache(blocked_id)

    def on_user_deactivated(self, user_id: int) -> None:
        user_vector_map.pop(user_id, None)
        vector_store.remove_user(user_id)
        # Flush toàn bộ cache vì kết quả của người khác có thể chứa user này
        self.flush_cache()

    def evict_user_cache(self, user_id: int) -> None:
        keys = list(redis_client.scan_iter(match=f"rec:user:{user_id}:k:*", count=100))
        if keys:
            redis_client.delete(*keys)

    def flush_cache(self) -> None:
        keys = list(redis_client.scan_iter(match="rec:user:*", count=500))
        if keys:
            redis_client.delete(*keys)


engine = RecommendationEngine()
