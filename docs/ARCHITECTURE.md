# Cairn — Local-First Contacts & Call History Archive
## Master Specification

This document covers the parts of the system that are architecture/process
rather than code: product spec, UX strategy, IA, search design, encryption
design, backup design, API spec, and deployment. The `android/` and
`backend/` folders contain the actual buildable code for the load-bearing
pieces (schema, FTS, crypto, repos, ViewModels, nav graph, key screens,
FastAPI service).

---

## 1. Product Specification

**One-line pitch:** A private, offline archive of your contacts and calls
that never expires, never phones home, and never asks you to trust a server
with your data.

**Non-goals:** caller-ID lookup of strangers, ads, analytics, social
features, cloud-as-default. The optional backend (Render) only ever stores
opaque encrypted blobs for backup/restore between a user's own devices.

**Target scale:** 100K → 5M call records on-device, 10+ years of history,
sub-100ms search on a mid-range phone.

**Platforms:** Android 8.0+ (API 26+), Compose Material 3, single APK,
no Play Services dependency for core function.

---

## 2. UX Strategy

Design language: **calm, dense, fast**. Not a "utility app" look — closer to
Cash App's confident color blocking, Linear's information density and
keyboard-first speed, Arc's playful chrome, Apple Contacts' typographic
restraint, Nothing OS's dot-matrix accents used sparingly for archival/retro
data-vault flavor.

Principles:
1. **Search is the front door.** Home screen surfaces a search bar first;
   typing immediately live-filters contacts + calls in one unified list.
2. **Time is a first-class dimension.** Every list can be scrubbed by a
   year rail (like a scrollbar that shows "2019 … 2024") for instant jump.
3. **One-handed, thumb-reachable.** Primary actions live in the bottom
   third; navigation is a minimal 4-item bottom bar + FAB for search.
4. **No modal dead-ends.** Everything opens as a bottom sheet or shared-
   element transition, never a blocking full-screen dialog.
5. **Numbers are honest.** Dashboard shows exact counts, not "1.2K+" —
   this is a vault, precision is the point.

---

## 3. Information Architecture

```
Splash
 └─ Onboarding (3 cards: Local-first / Encrypted / Your data never leaves)
     └─ Permissions (Contacts, Call Log, Phone state, Notifications)
         └─ Security Setup (PIN + optional biometric)
             └─ Home
                 ├─ Search (FAB, full-screen overlay)
                 ├─ Contacts (list, A–Z rail)
                 │   └─ Contact Detail
                 │       ├─ Call history for contact
                 │       ├─ Notes / Tags
                 │       └─ Edit
                 ├─ Timeline (Year → Month → Day drill-down)
                 │   └─ Call Detail
                 ├─ Dashboard (stats, heatmap)
                 ├─ Favorites
                 ├─ Tags (smart groups)
                 ├─ Archive Explorer (raw table browser w/ filters)
                 └─ Settings
                     ├─ Backup & Restore
                     ├─ Security
                     ├─ Appearance
                     └─ Database Health
```

Bottom nav: **Home · Timeline · Dashboard · Settings**, with a floating
center Search action (Cash App–style pill).

---

## 4. Database Schema (see `android/.../entity` for Kotlin source of truth)

Core tables:

- `contacts` (id, display_name, first_name, last_name, photo_uri,
  is_favorite, created_at, updated_at)
- `phone_numbers` (id, contact_id FK, number, normalized_number,
  label [mobile/home/work/other])
- `emails` (id, contact_id FK, address, label)
- `addresses` (id, contact_id FK, street, city, region, postal, country, label)
- `tags` (id, name, color)
- `contact_tags` (contact_id, tag_id) — join table
- `notes` (id, owner_type [contact/call], owner_id, body, created_at)
- `call_logs` (id, contact_id FK nullable, raw_number, normalized_number,
  call_type [incoming/outgoing/missed/rejected/blocked/unknown],
  timestamp_epoch, duration_seconds, sim_slot, sim_label, note, created_at)
- `call_logs_fts` — FTS4/5 virtual table mirroring searchable columns
  (contact_name, raw_number, note, tag_names) with `content=call_logs`
- `backup_metadata` (id, filename, created_at, size_bytes, device_name,
  schema_version, uploaded, remote_id)

Indexes (see `Migrations.kt` / DAO annotations):
- `call_logs(timestamp_epoch)` — timeline & range queries
- `call_logs(normalized_number)` — number search / grouping
- `call_logs(contact_id)` — contact detail joins
- `call_logs(call_type, timestamp_epoch)` — "missed calls in March"
- Composite covering index for pagination: `(timestamp_epoch DESC, id DESC)`

Partitioning strategy for 5M+ rows: no physical sharding needed — SQLite
with proper indexes + `LIMIT/OFFSET` keyset pagination handles this on
device flash storage. We avoid `OFFSET` for deep pages and use **keyset
pagination** (`WHERE timestamp_epoch < :lastSeen ORDER BY timestamp_epoch
DESC LIMIT 50`) via Room `PagingSource`.

---

## 5. Search Architecture

- Room + **FTS4** virtual table (`call_logs_fts`) content-linked to
  `call_logs` for zero-copy search over name/number/note.
- A lightweight **query parser** (`SearchQueryParser.kt`, see domain layer)
  turns natural phrases into structured filters before hitting SQL:
  - Numeric run of 4+ digits at end → "ends with" filter on
    `normalized_number`
  - `missed|incoming|outgoing|rejected` token → call_type filter
  - Month name / year token → timestamp range filter
  - `longer than N min` / `> N min` → duration filter
  - Remaining free text → FTS `MATCH` against name/note
- All filters compose into one Room `@RawQuery` / SupportSQLite query
  built with a query-builder, executed against covering indexes.
- Search-as-you-type is debounced 120ms and run on `Dispatchers.IO`,
  results streamed via `Flow` + Paging 3 so the UI never blocks.
- Target: <50ms for indexed exact/range filters, <150ms for FTS MATCH
  against 5M rows on a mid-range device (Snapdragon 6-series class).

---

## 6. Encryption Architecture

Two independent layers:

**A. At-rest DB encryption (SQLCipher)**
- Room opens the DB through `SupportFactory(passphrase)` from
  `net.zetetic:android-database-sqlcipher`.
- Passphrase = 256-bit key generated on first launch, stored in the
  **Android Keystore** (hardware-backed where available) wrapped with a
  key that requires biometric/PIN unlock (`BiometricPrompt` +
  `CryptoObject`).
- PIN fallback: PIN is never stored — it's used to derive/unwrap the
  Keystore-protected key via `BiometricManager.Authenticators.DEVICE_
  CREDENTIAL`.

**B. Backup encryption (client-side, before any network call)**
- Export DB → single file → compress (zstd/gzip) →
  **AES-256-GCM** encrypt with a key derived from a user backup
  passphrase via **Argon2id** (via `libsodium` bindings) or PBKDF2-SHA256
  (600k iterations) as a fallback.
- Random salt + nonce stored in a small unencrypted header alongside the
  encrypted blob (salt/nonce are not secret).
- The server (Render) receives only: `header + ciphertext`. It cannot
  decrypt — it never sees the passphrase, the derived key, or plaintext.
- Restoring on a new device re-derives the key from the same passphrase
  the user enters, decrypts locally, and rehydrates Room.

**Key non-negotiable:** if the user loses their backup passphrase, the
backup is unrecoverable by design (zero-knowledge). This is stated
explicitly in onboarding and the Backup screen.

---

## 7. Backup Architecture

1. `BackupWorker` (WorkManager, user-triggered or scheduled) exports the
   Room DB, encrypts it (see §6B), writes to app-private storage.
2. If "cloud backup" is enabled and network is available, uploads the
   encrypted blob via `POST /backup/upload` with a bearer JWT.
3. Local encrypted backups are always kept regardless of cloud settings
   (share-to-Drive/Files as a manual escape hatch requires no backend).
4. Restore flow: pick a backup (local file or `GET /backup/list` +
   `GET /backup/download/{id}`) → enter passphrase → decrypt → validate
   schema version → migrate if needed → replace/merge into Room inside a
   transaction with a pre-restore safety snapshot.
5. Sync (future): last-write-wins per-record sync using
   `updated_at` timestamps; out of scope for v1, backend schema leaves
   room for it (`backup_metadata.schema_version`).

---

## 8. Backend Architecture (Render free tier)

FastAPI, SQLite (upgradeable to Postgres via `DATABASE_URL`), JWT auth,
stores **opaque encrypted files only**. No decryption capability exists
anywhere in the backend code — this is a design invariant, not just a
policy.

See `backend/` for the runnable service, `Dockerfile`, and `render.yaml`.

Responsibilities: register/login, issue/verify JWT, accept/list/download/
delete encrypted backup blobs + metadata, health check. Nothing else.

---

## 9. API Specification

```
POST   /auth/register        { email, password }            -> 201
POST   /auth/login           { email, password }             -> { access_token }
GET    /auth/me              (Bearer)                        -> { id, email, created_at }

POST   /backup/upload        multipart: file, device_name    -> BackupMeta
GET    /backup/list          (Bearer)                        -> [BackupMeta]
GET    /backup/download/{id} (Bearer)                        -> binary stream
DELETE /backup/{id}          (Bearer)                        -> 204

GET    /health                                                -> { status: "ok" }
```

`BackupMeta = { id, filename, device_name, size_bytes, schema_version,
created_at }`

All endpoints except `/auth/*` and `/health` require
`Authorization: Bearer <jwt>`. Passwords hashed with bcrypt. JWT signed
HS256 with `JWT_SECRET` env var, 7-day expiry, refresh via re-login (kept
intentionally simple for a backup-only service).

---

## 10. Performance Plan by Scale

| Records | Strategy |
|---|---|
| 100K | Room + basic indexes, no special handling needed |
| 500K | Add covering index for timeline pagination, enable `PRAGMA journal_mode=WAL` |
| 1M | Paging 3 keyset pagination everywhere, FTS content table, batch-insert archival (500 rows/txn) |
| 5M | Above + periodic `PRAGMA optimize` / incremental vacuum via `DbMaintenanceWorker`, avoid `COUNT(*)` on hot paths (maintain a `stats_cache` table updated incrementally), avoid full-table `OFFSET` scans |

Database Health screen surfaces: DB file size, last vacuum date,
integrity check (`PRAGMA integrity_check`) result, and a "repair" flow that
restores from the most recent local encrypted backup if corruption is
detected.

---

## 11. Deployment (Android)

- `app/build.gradle.kts`: `minSdk 26`, `targetSdk 34`, R8 full mode,
  resource shrinking, `signingConfigs` from env-injected keystore for CI.
- Release build strips all logging (`Timber` no-op tree in release),
  no crash/analytics SDKs included at all.

## 12. Deployment (Backend, Render)

See `backend/render.yaml` — one-click Blueprint deploy:
1. Push `backend/` to a GitHub repo.
2. Render → New → Blueprint → point at repo → it reads `render.yaml`.
3. Set `JWT_SECRET` in the Render dashboard (marked `sync: false`).
4. Free tier Postgres or the bundled SQLite volume both work; `DATABASE_URL`
   env var switches between them automatically.

---

## 13. What's implemented as real code here vs. specified

**Implemented (runnable/compilable source, in `android/` and `backend/`):**
Room entities + DAOs + FTS setup, SQLCipher integration, keystore-backed
crypto module, backup encrypt/decrypt module, repository layer, core
ViewModels (Search, Timeline, Contacts, Dashboard), Navigation Compose
graph, theme + design tokens, Home/Search/Timeline/ContactDetail/Dashboard
composables, WorkManager archival + backup workers, full FastAPI backend
with auth/backup routers, Dockerfile, render.yaml, GitHub Actions CI.

**Specified but left as stubs/TODOs for you to flesh out:** remaining
screens (Onboarding, Permissions, Tags, Archive Explorer, Appearance, DB
Health polish), full biometric enrollment UI, heatmap chart rendering,
instrumented tests, and Play Store release signing — each has a clear
file/location and a `// TODO` describing exactly what belongs there, so
the project compiles and runs end-to-end today and is extendable without
re-architecting anything.
