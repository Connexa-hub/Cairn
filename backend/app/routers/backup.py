import os
import shutil
import uuid

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.deps import get_current_user
from app.db.session import get_db
from app.models.models import Backup, User
from app.models.schemas import BackupMetaResponse

router = APIRouter(prefix="/backup", tags=["backup"])


def _user_storage_dir(user_id: str) -> str:
    path = os.path.join(settings.storage_dir, user_id)
    os.makedirs(path, exist_ok=True)
    return path


@router.post("/upload", response_model=BackupMetaResponse)
async def upload_backup(
    file: UploadFile = File(...),
    device_name: str = Form(default="Unknown device"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Accepts an already-encrypted blob (AES-256-GCM ciphertext + header,
    produced on-device by BackupCrypto). This endpoint never sees, needs,
    or could derive the passphrase used to encrypt it — it just stores
    bytes.
    """
    max_bytes = settings.max_upload_mb * 1024 * 1024
    backup_id = str(uuid.uuid4())
    storage_dir = _user_storage_dir(current_user.id)
    dest_path = os.path.join(storage_dir, f"{backup_id}.enc")

    size = 0
    with open(dest_path, "wb") as out:
        while chunk := await file.read(1024 * 1024):
            size += len(chunk)
            if size > max_bytes:
                out.close()
                os.remove(dest_path)
                raise HTTPException(
                    status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                    detail=f"Backup exceeds {settings.max_upload_mb}MB limit",
                )
            out.write(chunk)

    backup = Backup(
        id=backup_id,
        user_id=current_user.id,
        filename=file.filename or "backup.enc",
        storage_path=dest_path,
        device_name=device_name,
        size_bytes=size,
        schema_version=1,
    )
    db.add(backup)
    db.commit()
    db.refresh(backup)
    return backup


@router.get("/list", response_model=list[BackupMetaResponse])
def list_backups(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return (
        db.query(Backup)
        .filter(Backup.user_id == current_user.id)
        .order_by(Backup.created_at.desc())
        .all()
    )


@router.get("/download/{backup_id}")
def download_backup(
    backup_id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    backup = (
        db.query(Backup)
        .filter(Backup.id == backup_id, Backup.user_id == current_user.id)
        .first()
    )
    if not backup or not os.path.exists(backup.storage_path):
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Backup not found")

    return FileResponse(
        backup.storage_path,
        media_type="application/octet-stream",
        filename=backup.filename,
    )


@router.delete("/{backup_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_backup(
    backup_id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    backup = (
        db.query(Backup)
        .filter(Backup.id == backup_id, Backup.user_id == current_user.id)
        .first()
    )
    if not backup:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Backup not found")

    if os.path.exists(backup.storage_path):
        os.remove(backup.storage_path)
    db.delete(backup)
    db.commit()
    return None
