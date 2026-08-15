from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.db.session import Base, engine
from app.models import models  # noqa: F401 ensures models are registered before create_all
from app.models.schemas import HealthResponse
from app.routers import auth, backup

Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Cairn Backup Service",
    description=(
        "A minimal, zero-knowledge backup/restore service for the Cairn "
        "Android app. Stores only encrypted blobs; has no capability to "
        "decrypt user data."
    ),
    version="1.0.0",
)

# Restrict this in production to your own client if you add a web dashboard;
# the Android app talks to this over plain HTTPS with a bearer token, not cookies,
# so permissive CORS here does not expose session data.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(backup.router)


@app.get("/health", response_model=HealthResponse, tags=["health"])
def health():
    return HealthResponse(status="ok")
