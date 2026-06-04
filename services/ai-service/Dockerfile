FROM python:3.10-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY model/ ./model/

COPY app/ ./app/
COPY *.py ./

EXPOSE 8000 50051

CMD ["python", "main.py"]
