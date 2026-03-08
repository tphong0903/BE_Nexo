import torch
import os
from transformers import AutoTokenizer, AutoModelForSequenceClassification

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

model_path = os.path.join(BASE_DIR, "model", "savedModel")

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

print("Loading model on:", device)

tokenizer = AutoTokenizer.from_pretrained(
    model_path,
    local_files_only=True
)

model = AutoModelForSequenceClassification.from_pretrained(
    model_path,
    local_files_only=True
)

model.to(device)
model.eval()


def predict(text: str):

    inputs = tokenizer(
        text,
        return_tensors="pt",
        truncation=True,
        padding=True,
        max_length=48
    ).to(device)

    with torch.no_grad():

        outputs = model(**inputs)

    probs = torch.softmax(outputs.logits, dim=-1)

    pred = torch.argmax(probs).item()

    confidence = probs[0][pred].item()

    label = "negative" if pred == 1 else "normal"

    return {
        "label": label,
        "confidence": confidence
    }