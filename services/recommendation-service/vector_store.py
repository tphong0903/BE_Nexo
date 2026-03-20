from __future__ import annotations

import numpy as np

try:
    import faiss  # type: ignore
except ImportError:  # pragma: no cover
    faiss = None


class VectorStore:
    def __init__(self, dim: int):
        self.dim = dim
        self.user_ids: list[int] = []
        self.vectors = np.empty((0, dim), dtype=np.float32)
        self.faiss_index = None

    def _rebuild_index(self) -> None:
        if faiss is None:
            self.faiss_index = None
            return

        if self.vectors.shape[0] == 0:
            self.faiss_index = None
            return

        index = faiss.IndexFlatIP(self.dim)
        index.add(self.vectors)
        self.faiss_index = index

    def rebuild(self, user_ids: list[int], vectors: np.ndarray) -> None:
        if vectors.dtype != np.float32:
            vectors = vectors.astype(np.float32)
        if vectors.ndim != 2 or vectors.shape[1] != self.dim:
            raise ValueError("Invalid vectors shape")

        self.user_ids = user_ids
        self.vectors = vectors
        self._rebuild_index()

    def update_one(self, user_id: int, vector: np.ndarray) -> None:
        vector = vector.astype(np.float32).reshape(1, -1)
        if vector.shape[1] != self.dim:
            raise ValueError("Invalid vector dim")

        try:
            idx = self.user_ids.index(user_id)
            self.vectors[idx] = vector[0]
        except ValueError:
            self.user_ids.append(user_id)
            self.vectors = np.vstack([self.vectors, vector])

        self._rebuild_index()

    def _search_numpy(self, query: np.ndarray, top_k: int) -> list[int]:
        scores = np.matmul(self.vectors, query.T).reshape(-1)
        order = np.argsort(-scores)
        return [self.user_ids[int(i)] for i in order[:top_k]]

    def _search_faiss(self, query: np.ndarray, top_k: int) -> list[int]:
        if self.faiss_index is None:
            return []
        _, idx = self.faiss_index.search(query, top_k)
        ids: list[int] = []
        for i in idx[0]:
            if i < 0 or i >= len(self.user_ids):
                continue
            ids.append(self.user_ids[int(i)])
        return ids

    def search(self, query: np.ndarray, top_k: int, exclude_ids: set[int] | None = None) -> list[int]:
        if self.vectors.shape[0] == 0:
            return []

        exclude_ids = exclude_ids or set()
        query = query.astype(np.float32).reshape(1, -1)

        raw_ids = self._search_faiss(query, top_k + len(exclude_ids) + 5) if faiss else self._search_numpy(
            query, top_k + len(exclude_ids) + 5
        )

        result: list[int] = []
        for uid in raw_ids:
            if uid in exclude_ids:
                continue
            result.append(uid)
            if len(result) >= top_k:
                break
        return result
