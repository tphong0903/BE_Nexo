from fastapi import FastAPI
from pydantic import BaseModel
from app.predictor import predict

app = FastAPI()

class TextRequest(BaseModel):
    text: str


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