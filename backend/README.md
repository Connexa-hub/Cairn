# Cairn Backup Service

Minimal FastAPI backend for the Cairn Android app. It does exactly four
things: register/login users, and store/list/retrieve/delete **already
encrypted** backup blobs. It has no code path that can decrypt a backup —
the encryption key never leaves the user's device (see `BackupCrypto.kt`
in the Android app).

## Run locally

```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
cp .env.example .env          # edit JWT_SECRET at minimum
uvicorn app.main:app --reload
```

Or with Docker:

```bash
docker compose up --build
```

Visit `http://localhost:8000/docs` for interactive OpenAPI docs.

## Run tests

```bash
pip install pytest httpx
pytest app/tests -v
```

## Deploy to Render (free tier)

1. Push the whole repo (not just `backend/`) to GitHub — Render needs to
   see `Dockerfile` and `render.yaml` together, and `dockerContext: .`
   in `render.yaml` is relative to `backend/`.
2. In the Render dashboard: **New → Blueprint**, connect your repo. Since
   `render.yaml` lives at `backend/render.yaml` rather than the repo
   root, set **Blueprint Path** to `backend/render.yaml` on the setup
   screen. Render then reads it and provisions the web service + a free
   Postgres database automatically — no persistent disk needed, since
   backups are stored as rows in that Postgres database (see Security
   notes below).
3. Set the `JWT_SECRET` environment variable in the Render dashboard
   (marked `sync: false` in the blueprint so it's never committed).
   Generate one with `openssl rand -hex 32`.
4. Deploy. Health check is `GET /health`.
5. Point the Android app's `Settings → Backup` at your Render URL.

### Environment variables

| Variable | Purpose | Default |
|---|---|---|
| `JWT_SECRET` | Signs auth tokens — set a real random value | *(required in prod)* |
| `DATABASE_URL` | `sqlite:///...` or `postgresql://...` | `sqlite:///./cairn_backend.db` |
| `MAX_UPLOAD_MB` | Per-backup size cap (kept modest — see note below) | `50` |
| `ACCESS_TOKEN_EXPIRE_MINUTES` | JWT lifetime | `10080` (7 days) |

## API

See `docs/ARCHITECTURE.md` §9 in the repo root for the full spec, or just
open `/docs` once running — FastAPI generates it from the route
definitions in `app/routers/`.

## Security notes

- Passwords are hashed with bcrypt, never stored or logged in plaintext.
- Backup blobs are stored as `bytea` rows in Postgres, not on disk.
  This is deliberate, not incidental: Render's free-tier web services
  don't support persistent disks at all, but the free Postgres database
  does persist — so this is what keeps backups durable across redeploys
  on the free tier without needing a paid plan. It does mean
  `MAX_UPLOAD_MB` should stay modest, since free-tier Postgres caps out
  around 1GB of total storage. Nothing about a blob's contents is ever
  parsed — it's opaque ciphertext either way.
- CORS is permissive (`*`) because auth is bearer-token based, not cookie
  based — there's no session to leak via CSRF. Tighten this if you add a
  web dashboard that relies on cookies.
- This service is optional. The Android app is fully functional offline
  without ever talking to it.
