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
    storage_dir: str = "./storage"
    max_upload_mb: int = 200

    class Config:
        env_file = ".env"


settings = Settings()
