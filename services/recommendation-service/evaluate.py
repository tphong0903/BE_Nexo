from __future__ import annotations

import numpy as np
import torch
import torch.nn.functional as F
from sklearn.metrics import roc_auc_score, average_precision_score
from torch_geometric.utils import negative_sampling

from config import settings
from data_loader import load_follows, load_users
from feature_engineering import build_graph_stats, user_feature_vector
from model import GraphSAGE


def evaluate() -> None:
    users = load_users()
    follows = load_follows()

    active_follows = [f for f in follows if (f.status or "ACTIVE").upper() == "ACTIVE"]
    user_ids = [u.id for u in users]
    user_id_to_idx = {uid: i for i, uid in enumerate(user_ids)}

    valid_edges = [
        (user_id_to_idx[f.follower_id], user_id_to_idx[f.following_id])
        for f in active_follows
        if f.follower_id in user_id_to_idx and f.following_id in user_id_to_idx
    ]

    if len(valid_edges) < 10:
        print("Không đủ edges để đánh giá (cần ít nhất 10 follow).")
        return

    np.random.shuffle(valid_edges)
    split = int(len(valid_edges) * 0.8)
    train_edges = valid_edges[:split]
    test_edges = valid_edges[split:]

    print(f"Total edges: {len(valid_edges)} | Train: {len(train_edges)} | Test: {len(test_edges)}")

    stats = build_graph_stats(users, follows)
    features = np.vstack([user_feature_vector(u, stats) for u in users]).astype(np.float32)
    x = torch.tensor(features, dtype=torch.float32)

    train_edge_index = torch.tensor(train_edges, dtype=torch.long).t().contiguous()
    train_edge_index = torch.cat([train_edge_index, train_edge_index.flip(0)], dim=1)

    model = GraphSAGE(settings.feature_dim, settings.hidden_dim, settings.embedding_dim)
    optimizer = torch.optim.Adam(model.parameters(), lr=settings.training_lr)

    for epoch in range(settings.training_epochs):
        model.train()
        z = model(x, train_edge_index)
        z_norm = F.normalize(z, p=2, dim=1)

        neg_edge = negative_sampling(
            edge_index=train_edge_index,
            num_nodes=x.size(0),
            num_neg_samples=train_edge_index.size(1),
            method="sparse",
        )
        pos_score = (z_norm[train_edge_index[0]] * z_norm[train_edge_index[1]]).sum(dim=1)
        neg_score = (z_norm[neg_edge[0]] * z_norm[neg_edge[1]]).sum(dim=1)

        loss = (
            F.binary_cross_entropy_with_logits(pos_score, torch.ones_like(pos_score))
            + F.binary_cross_entropy_with_logits(neg_score, torch.zeros_like(neg_score))
        )
        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        if (epoch + 1) % 10 == 0:
            print(f"  Epoch {epoch + 1}/{settings.training_epochs} | Loss: {loss.item():.4f}")

    model.eval()
    with torch.no_grad():
        z = F.normalize(model(x, train_edge_index), p=2, dim=1)

    test_edge_tensor = torch.tensor(test_edges, dtype=torch.long).t()
    neg_test = negative_sampling(
        edge_index=train_edge_index,
        num_nodes=x.size(0),
        num_neg_samples=len(test_edges),
        method="sparse",
    )

    with torch.no_grad():
        pos_scores = torch.sigmoid((z[test_edge_tensor[0]] * z[test_edge_tensor[1]]).sum(dim=1)).numpy()
        neg_scores = torch.sigmoid((z[neg_test[0]] * z[neg_test[1]]).sum(dim=1)).numpy()

    y_true = np.concatenate([np.ones(len(pos_scores)), np.zeros(len(neg_scores))])
    y_score = np.concatenate([pos_scores, neg_scores])

    auc = roc_auc_score(y_true, y_score)
    ap = average_precision_score(y_true, y_score)
    hits_at_10 = _hits_at_k(z, test_edges, train_edges, k=10)
    hits_at_20 = _hits_at_k(z, test_edges, train_edges, k=20)

    print("\n=== KẾT QUẢ ===")
    print(f"AUC-ROC : {auc:.4f}  (>0.70 tạm được, >0.85 tốt)")
    print(f"AP      : {ap:.4f}  (càng cao càng tốt)")
    print(f"Hits@10 : {hits_at_10:.4f}  (>0.10 tốt)")
    print(f"Hits@20 : {hits_at_20:.4f}")


def _hits_at_k(z: torch.Tensor, test_edges: list, train_edges: list, k: int) -> float:
    train_set = set(map(tuple, train_edges))
    hits = 0
    for src, dst in test_edges:
        scores = (z[src] * z).sum(dim=1)
        scores[src] = -1e9
        for ts, td in train_set:
            if ts == src:
                scores[td] = -1e9
        if dst in scores.topk(k).indices.tolist():
            hits += 1
    return hits / len(test_edges) if test_edges else 0.0


if __name__ == "__main__":
    evaluate()
