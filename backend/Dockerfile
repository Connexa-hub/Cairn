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

# No storage directory needed — encrypted backup blobs are stored as rows
# in the Postgres database (app/models/models.py — Backup.data), not on
# disk, since Render's free-tier web services have no persistent disk.

CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT}"]
