import uuid
from datetime import datetime, timezone

from sqlalchemy import Column, String, Integer, DateTime, ForeignKey, BigInteger, LargeBinary
from sqlalchemy.orm import relationship

from app.db.session import Base


def gen_uuid() -> str:
    return str(uuid.uuid4())


class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, default=gen_uuid)
    email = Column(String, unique=True, index=True, nullable=False)
    hashed_password = Column(String, nullable=False)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))

    backups = relationship("Backup", back_populates="owner", cascade="all, delete-orphan")


class Backup(Base):
    """
    Stores metadata + the opaque encrypted blob itself, as a bytea column
    in Postgres rather than a file on disk. This is deliberate: Render's
    free-tier web services don't support persistent disks, but the free
    Postgres database IS persistent — so storing ciphertext as a DB column
    keeps backups durable across redeploys without needing a paid plan.
    This service has no code path capable of decrypting `data`'s contents
    — that key never exists server-side.
    """
    __tablename__ = "backups"

    id = Column(String, primary_key=True, default=gen_uuid)
    user_id = Column(String, ForeignKey("users.id"), nullable=False, index=True)
    filename = Column(String, nullable=False)
    data = Column(LargeBinary, nullable=False)
    device_name = Column(String, nullable=True)
    size_bytes = Column(BigInteger, nullable=False)
    schema_version = Column(Integer, default=1)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))

    owner = relationship("User", back_populates="backups")

