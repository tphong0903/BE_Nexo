import pandas as pd
import numpy as np
from scipy.sparse import csr_matrix
from scipy.sparse.linalg import svds
from sqlalchemy import create_engine
import redis
from fastapi import FastAPI, BackgroundTasks
from pydantic import BaseModel
import time
from app.predictor import predict

app = FastAPI()

DB_URL = "postgresql+psycopg2://user:pass@localhost:5433/userdb"
engine = create_engine(DB_URL)

redis_client = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)

class TextRequest(BaseModel):
    text: str

def train_and_push_to_redis(top_k: int = 10):
    query = """
        WITH ActiveUsers AS (
            SELECT user_id 
            FROM user_post_scores 
            WHERE updated_at >= NOW() - INTERVAL '30 days'
              AND post_id IS NOT NULL
            GROUP BY user_id 
            HAVING COUNT(post_id) >= 5
        )
        SELECT user_id, post_id, scores AS interaction_score 
        FROM user_post_scores 
        WHERE updated_at >= NOW() - INTERVAL '30 days'
          AND post_id IS NOT NULL
          AND user_id IN (SELECT user_id FROM ActiveUsers)
    """
    df = pd.read_sql(query, engine)

    if df.empty:
        return

    users = df['user_id'].astype('category')
    posts = df['post_id'].astype('category')

    user_cat = users.cat.codes
    post_cat = posts.cat.codes

    sparse_matrix = csr_matrix(
        (df['interaction_score'], (user_cat, post_cat)),
        shape=(len(users.cat.categories), len(posts.cat.categories))
    )

    sparse_matrix_float = sparse_matrix.asfptype()

    k = min(sparse_matrix.shape) - 1
    k = min(k, 20)

    if k > 0:
        U, sigma, Vt = svds(sparse_matrix_float, k=k)
        sigma = np.diag(sigma)
        predicted_ratings = np.dot(np.dot(U, sigma), Vt)
    else:
        predicted_ratings = sparse_matrix.toarray()

    predicted_df = pd.DataFrame(
        predicted_ratings,
        index=users.cat.categories,
        columns=posts.cat.categories
    )

    current_time = int(time.time() * 1000)

    for user_id in predicted_df.index:
        user_predictions = predicted_df.loc[user_id]
        interacted_posts = df[df['user_id'] == user_id]['post_id'].tolist()

        recommendations = user_predictions.drop(
            interacted_posts,
            errors='ignore'
        ).sort_values(ascending=False).head(top_k)

        feed_key = f"feed:{user_id}"

        for post_id, score in recommendations.items():
            smart_score = current_time + (float(score) * 1000)
            redis_client.zadd(feed_key, {str(post_id): smart_score})
            redis_client.zremrangebyrank(feed_key, 0, -51)

def train_reels_and_push_to_redis(top_k: int = 10):
    query = """
        WITH ActiveUsers AS (
            SELECT user_id 
            FROM user_post_scores 
            WHERE updated_at >= NOW() - INTERVAL '30 days'
              AND reel_id IS NOT NULL
            GROUP BY user_id 
            HAVING COUNT(reel_id) >= 5
        )
        SELECT user_id, reel_id, scores AS interaction_score 
        FROM user_post_scores 
        WHERE updated_at >= NOW() - INTERVAL '30 days'
          AND reel_id IS NOT NULL
          AND user_id IN (SELECT user_id FROM ActiveUsers)
    """
    df = pd.read_sql(query, engine)

    if df.empty:
        return

    users = df['user_id'].astype('category')
    reels = df['reel_id'].astype('category')

    user_cat = users.cat.codes
    reel_cat = reels.cat.codes

    sparse_matrix = csr_matrix(
        (df['interaction_score'], (user_cat, reel_cat)),
        shape=(len(users.cat.categories), len(reels.cat.categories))
    )

    sparse_matrix_float = sparse_matrix.asfptype()

    k = min(sparse_matrix.shape) - 1
    k = min(k, 20)

    if k > 0:
        U, sigma, Vt = svds(sparse_matrix_float, k=k)
        sigma = np.diag(sigma)
        predicted_ratings = np.dot(np.dot(U, sigma), Vt)
    else:
        predicted_ratings = sparse_matrix.toarray()

    predicted_df = pd.DataFrame(
        predicted_ratings,
        index=users.cat.categories,
        columns=reels.cat.categories
    )

    current_time = int(time.time() * 1000)

    for user_id in predicted_df.index:
        user_predictions = predicted_df.loc[user_id]
        interacted_reels = df[df['user_id'] == user_id]['reel_id'].tolist()

        recommendations = user_predictions.drop(
            interacted_reels,
            errors='ignore'
        ).sort_values(ascending=False).head(top_k)

        feed_key = f"feed:reel:{user_id}"

        for reel_id, score in recommendations.items():
            smart_score = current_time + (float(score) * 1000)
            redis_client.zadd(feed_key, {str(reel_id): smart_score})
            redis_client.zremrangebyrank(feed_key, 0, -51)

@app.get("/")
def health():
    return {"service": "ai-service", "status": "running"}

@app.post("/predict")
def predictText(req: TextRequest):
    result = predict(req.text)
    return {
        "text": req.text,
        "violation": result
    }

@app.post("/train-recommendations")
def trigger_training(background_tasks: BackgroundTasks):
    background_tasks.add_task(train_and_push_to_redis)
    return {"message": "Đang xử lý train bài viết"}

@app.post("/train-reels-recommendations")
def trigger_reels_training(background_tasks: BackgroundTasks):
    background_tasks.add_task(train_reels_and_push_to_redis)
    return {"message": "Đang xử lý train reels"}