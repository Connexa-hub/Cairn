import uuid

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from fastapi.responses import Response
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.deps import get_current_user
from app.db.session import get_db
from app.models.models import Backup, User
from app.models.schemas import BackupMetaResponse

router = APIRouter(prefix="/backup", tags=["backup"])


@router.post("/upload", response_model=BackupMetaResponse)
async def upload_backup(
    file: UploadFile = File(...),
    device_name: str = Form(default="Unknown device"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Accepts an already-encrypted blob (AES-256-GCM ciphertext + header,
    produced on-device by BackupCrypto) and stores it as a bytea column in
    Postgres. This endpoint never sees, needs, or could derive the
    passphrase used to encrypt it — it just stores bytes.

    Stored in the database rather than on disk deliberately: Render's
    free-tier web services have no persistent disk, but the free Postgres
    database does persist, so this is what keeps backups durable on the
    free tier.
    """
    max_bytes = settings.max_upload_mb * 1024 * 1024

    chunks = []
    size = 0
    while chunk := await file.read(1024 * 1024):
        size += len(chunk)
        if size > max_bytes:
            raise HTTPException(
                status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                detail=f"Backup exceeds {settings.max_upload_mb}MB limit",
            )
        chunks.append(chunk)
    data = b"".join(chunks)

    backup = Backup(
        id=str(uuid.uuid4()),
        user_id=current_user.id,
        filename=file.filename or "backup.enc",
        data=data,
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
    if not backup:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Backup not found")

    return Response(
        content=backup.data,
        media_type="application/octet-stream",
        headers={"Content-Disposition": f'attachment; filename="{backup.filename}"'},
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

    db.delete(backup)
    db.commit()
    return None
