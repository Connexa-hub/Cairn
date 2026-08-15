from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """
    All configuration comes from environment variables (see .env.example).
    Nothing here ever configures encryption of the *contents* of a backup —
    this service only ever stores and serves opaque ciphertext blobs that
    were already encrypted on the user's device.
    """
    jwt_secret: str = "changeme-generate-a-real-secret"
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 10080  # 7 days

    database_url: str = "sqlite:///./cairn_backend.db"
    # Backup blobs are stored as bytea rows in this same database (see
    # app/models/models.py — Backup.data), not on disk — Render's free-tier
    # web services have no persistent disk, so this keeps storage durable
    # without needing a paid plan. max_upload_mb should stay modest on a
    # free-tier Postgres instance (~1GB total storage).
    max_upload_mb: int = 50

    class Config:
        env_file = ".env"


settings = Settings()
