import pandas as pd
import numpy as np
from scipy.sparse import csr_matrix
from scipy.sparse.linalg import svds
from sqlalchemy import create_engine
import redis
from fastapi import FastAPI, BackgroundTasks
from pydantic import BaseModel
import time
import logging
from app.predictor import predict
from apscheduler.schedulers.background import BackgroundScheduler
from contextlib import asynccontextmanager
import os

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

db_user = os.getenv("DB_USER", "user")
db_pass = os.getenv("DB_PASS", "pass")
db_host = "user-db"
db_name = "userdb"
DB_URL = f"postgresql+psycopg2://{db_user}:{db_pass}@{db_host}:5432/{db_name}"

engine = create_engine(DB_URL)

redis_host = os.getenv("REDIS_HOST", "redis")
redis_port = int(os.getenv("REDIS_PORT", 6379))
redis_password = os.getenv("REDIS_PASSWORD", "Admin@123")

redis_client = redis.Redis(
    host=redis_host,
    port=redis_port,
    password=redis_password,
    db=0,
    decode_responses=True
)


class TextRequest(BaseModel):
    text: str


def train_and_push_to_redis(top_k: int = 10):
    logger.info("Bat dau tien trinh train recommendations cho Posts")
    try:
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
            logger.warning("Khong co du lieu tuong tac hop le de train Posts trong 30 ngay qua")
            return

        logger.info(f"Lay thanh cong {len(df)} ban ghi tuong tac Posts tu Database")

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
            logger.info(f"Thuc hien SVD voi k={k} cho Posts")
            U, sigma, Vt = svds(sparse_matrix_float, k=k)
            sigma = np.diag(sigma)
            predicted_ratings = np.dot(np.dot(U, sigma), Vt)
        else:
            logger.info("Kich thuoc ma tran qua nho, bo qua SVD cho Posts")
            predicted_ratings = sparse_matrix.toarray()

        predicted_df = pd.DataFrame(
            predicted_ratings,
            index=users.cat.categories,
            columns=posts.cat.categories
        )

        current_time = int(time.time() * 1000)
        user_count = 0

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

            user_count += 1

        logger.info(f"Hoan thanh push ket qua train Posts vao Redis cho {user_count} users")

    except Exception as e:
        logger.error(f"Loi trong qua trinh train Posts: {str(e)}")


def train_reels_and_push_to_redis(top_k: int = 10):
    logger.info("Bat dau tien trinh train recommendations cho Reels")
    try:
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
            logger.warning("Khong co du lieu tuong tac hop le de train Reels trong 30 ngay qua")
            return

        logger.info(f"Lay thanh cong {len(df)} ban ghi tuong tac Reels tu Database")

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
            logger.info(f"Thuc hien SVD voi k={k} cho Reels")
            U, sigma, Vt = svds(sparse_matrix_float, k=k)
            sigma = np.diag(sigma)
            predicted_ratings = np.dot(np.dot(U, sigma), Vt)
        else:
            logger.info("Kich thuoc ma tran qua nho, bo qua SVD cho Reels")
            predicted_ratings = sparse_matrix.toarray()

        predicted_df = pd.DataFrame(
            predicted_ratings,
            index=users.cat.categories,
            columns=reels.cat.categories
        )

        current_time = int(time.time() * 1000)
        user_count = 0

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

            user_count += 1

        logger.info(f"Hoan thanh push ket qua train Reels vao Redis cho {user_count} users")

    except Exception as e:
        logger.error(f"Loi trong qua trinh train Reels: {str(e)}")


def train_trending_and_push_to_redis():
    logger.info("Bat dau tien trinh tinh toan Trending chung")
    try:
        post_query = """
            SELECT p.id, (p.like_quantity + p.comment_quantity * 2) AS trend_score
            FROM post_model p
            JOIN users u ON p.user_id = u.id
            WHERE p.created_at >= NOW() - INTERVAL '7 days' 
              AND p.is_active = true 
              AND p.visibility = 'PUBLIC'
              AND u.is_private = false
            ORDER BY trend_score DESC
            LIMIT 50
        """
        post_df = pd.read_sql(post_query, engine)

        if not post_df.empty:
            redis_client.delete("trending:posts")
            for index, row in post_df.iterrows():
                redis_client.zadd("trending:posts", {str(int(row['id'])): float(row['trend_score'])})
            logger.info(f"Da push {len(post_df)} Posts vao danh sach trending:posts")
        else:
            logger.warning("Khong tim thay bai post nao de dua vao Trending")

        reel_query = """
            SELECT r.id, (r.like_quantity + r.comment_quantity * 2) AS trend_score
            FROM reel_model r
            JOIN users u ON r.user_id = u.id
            WHERE r.created_at >= NOW() - INTERVAL '7 days' 
              AND r.is_active = true 
              AND r.visibility = 'PUBLIC'
              AND u.is_private = false
            ORDER BY trend_score DESC
            LIMIT 50
        """
        reel_df = pd.read_sql(reel_query, engine)

        if not reel_df.empty:
            redis_client.delete("trending:reels")
            for index, row in reel_df.iterrows():
                redis_client.zadd("trending:reels", {str(int(row['id'])): float(row['trend_score'])})
            logger.info(f"Da push {len(reel_df)} Reels vao danh sach trending:reels")
        else:
            logger.warning("Khong tim thay reel nao de dua vao Trending")

    except Exception as e:
        logger.error(f"Loi trong qua trinh xu ly Trending: {str(e)}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    scheduler = BackgroundScheduler()

    scheduler.add_job(train_and_push_to_redis, 'interval', hours=1)

    scheduler.add_job(train_reels_and_push_to_redis, 'interval', hours=1)

    scheduler.add_job(train_trending_and_push_to_redis, 'cron', hour=2, minute=0)
    scheduler.start()
    logger.info("Background Scheduler đã được khởi động!")
    yield
    scheduler.shutdown()
    logger.info("Background Scheduler đã dừng.")

app = FastAPI(lifespan=lifespan)

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
    logger.info("Nhan request trigger train recommendations (Posts)")
    background_tasks.add_task(train_and_push_to_redis)
    return {"message": "Đang xử lý train bài viết"}


@app.post("/train-reels-recommendations")
def trigger_reels_training(background_tasks: BackgroundTasks):
    logger.info("Nhan request trigger train recommendations (Reels)")
    background_tasks.add_task(train_reels_and_push_to_redis)
    return {"message": "Đang xử lý train reels"}


@app.post("/train-trending")
def trigger_trending(background_tasks: BackgroundTasks):
    logger.info("Nhan request trigger train Trending")
    background_tasks.add_task(train_trending_and_push_to_redis)
    return {"message": "Đang xử lý trending chung"}