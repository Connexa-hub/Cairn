import io
import os

os.environ["DATABASE_URL"] = "sqlite:///./test_cairn_backend.db"
os.environ["JWT_SECRET"] = "test-secret"

from fastapi.testclient import TestClient  # noqa: E402

from app.main import app  # noqa: E402

client = TestClient(app)


def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json() == {"status": "ok"}


def test_register_login_me():
    r = client.post("/auth/register", json={"email": "a@example.com", "password": "hunter22222"})
    assert r.status_code == 201
    assert r.json()["email"] == "a@example.com"

    # Duplicate registration is rejected
    r_dup = client.post("/auth/register", json={"email": "a@example.com", "password": "hunter22222"})
    assert r_dup.status_code == 409

    r = client.post("/auth/login", json={"email": "a@example.com", "password": "hunter22222"})
    assert r.status_code == 200
    token = r.json()["access_token"]

    r = client.get("/auth/me", headers={"Authorization": f"Bearer {token}"})
    assert r.status_code == 200
    assert r.json()["email"] == "a@example.com"


def _login(email="b@example.com", password="hunter22222"):
    client.post("/auth/register", json={"email": email, "password": password})
    r = client.post("/auth/login", json={"email": email, "password": password})
    return r.json()["access_token"]


def test_backup_upload_list_download_delete():
    token = _login()
    headers = {"Authorization": f"Bearer {token}"}

    fake_ciphertext = io.BytesIO(b"totally-encrypted-bytes-not-really-but-opaque-to-server")
    r = client.post(
        "/backup/upload",
        headers=headers,
        files={"file": ("cairn_backup.enc", fake_ciphertext, "application/octet-stream")},
        data={"device_name": "Pixel 8"},
    )
    assert r.status_code == 200
    meta = r.json()
    assert meta["device_name"] == "Pixel 8"
    backup_id = meta["id"]

    r = client.get("/backup/list", headers=headers)
    assert r.status_code == 200
    assert any(b["id"] == backup_id for b in r.json())

    r = client.get(f"/backup/download/{backup_id}", headers=headers)
    assert r.status_code == 200
    assert r.content == b"totally-encrypted-bytes-not-really-but-opaque-to-server"

    r = client.delete(f"/backup/{backup_id}", headers=headers)
    assert r.status_code == 204

    r = client.get("/backup/list", headers=headers)
    assert all(b["id"] != backup_id for b in r.json())


def test_unauthenticated_backup_access_rejected():
    r = client.get("/backup/list")
    assert r.status_code in (401, 403)
