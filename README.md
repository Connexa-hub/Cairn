# Cairn — Local-First Contacts & Call History Archive

A private, offline-first Android app that archives your contacts and call
history permanently, encrypted, on your own device — with an optional,
zero-knowledge encrypted backup service you can self-host for free on
Render.

No ads. No analytics. No telemetry. No phone-number lookup service. No
cloud dependency for normal use.

```
cairn/
├── android/     Kotlin/Compose app — the whole product, works fully offline
├── backend/     Optional FastAPI backup/restore service (Render-deployable)
├── docs/        Full architecture & product spec
└── .github/     CI for both android/ and backend/
```

## Start here

- **Product spec, UX strategy, database schema, search/encryption/backup
  architecture, API spec** → [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- **Run the Android app locally** → open `android/` in Android Studio
  (Koala+), let it sync, run on a device/emulator running API 26+.
- **Get a real, installable release build** → this sandbox has no network
  access, so nothing here has actually been compiled yet. GitHub Actions
  does have network access and is where the real build happens — see
  [`RELEASE.md`](RELEASE.md) for the exact push-to-tag-to-installable-APK
  steps.
- **Run the backend** (optional, only needed for cross-device encrypted
  backup) → see [`backend/README.md`](backend/README.md).

## Why this exists

Android's system call log is not an archive — it gets trimmed, cleared,
or lost on a factory reset. Cairn reads it continuously and keeps every
record forever, locally, encrypted with SQLCipher behind your device's
biometric/PIN lock, indexed for instant full-text search
(`"missed calls in March"`, `"calls ending in 4421"`, `"David 2023"`)
even across millions of rows and a decade of history.

## Security model, briefly

- The on-device database is encrypted at rest (SQLCipher) with a key held
  in the Android Keystore, unlocked via biometrics/device credential.
- Backups are encrypted client-side (AES-256-GCM, PBKDF2-derived key from
  a passphrase you choose) *before* anything touches the network.
- The optional backend only ever stores that ciphertext — it has no key
  and no way to decrypt your data, ever. If you lose your backup
  passphrase, that backup is unrecoverable by design.

Full detail in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) §6–7.

## Project status

This is a working foundation, not a finished, store-ready app: the core
architecture (schema, FTS search, encryption, repositories, navigation,
and the primary screens — Home/Search/Timeline/Contact Detail/Dashboard)
is implemented and wired end-to-end. Several screens are intentionally
left as clearly-marked simple stubs (Onboarding pager, Tags drag-assign
UI, Archive Explorer filter chips, launcher icon artwork) — see inline
`// TODO`s. The backend is complete and tested. **None of it has been
compiled yet** — this was written in a sandbox with no network access;
see [`RELEASE.md`](RELEASE.md) to get a real compiled, signed, installable
build via GitHub Actions.

## License

MIT — see [`LICENSE`](LICENSE).
