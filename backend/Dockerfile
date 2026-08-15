FROM python:3.12-slim

WORKDIR /app

# System deps for psycopg2 (Postgres) — harmless if you end up on SQLite only
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc libpq-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

# Render injects $PORT at runtime; default to 8000 for local docker run
ENV PORT=8000
EXPOSE 8000

# Persistent storage for encrypted backup blobs — mount a volume/disk here in production
RUN mkdir -p /app/storage

CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT}"]
