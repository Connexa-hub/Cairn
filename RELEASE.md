# Releasing Cairn

This repo builds nothing locally by design of how you got it — the actual
compile/link/test/sign happens on GitHub's runners, which have the network
access to pull the Android SDK, Gradle, and every dependency. This doc is
the exact sequence to go from "code in a folder" to "a real APK someone can
install," and to get the backend live on Render.

## 1. Push this repo to GitHub

```bash
cd cairn
git init
git add .
git commit -m "Initial commit: Cairn app + backend"
gh repo create cairn --private --source=. --push
# or: create the repo on github.com, then
#   git remote add origin git@github.com:<you>/cairn.git
#   git branch -M main && git push -u origin main
```

At this point **Android CI** and **Backend CI**
(`.github/workflows/android-ci.yml`, `backend-ci.yml`) already run
automatically on every push/PR — check the **Actions** tab. This is your
first real signal the code actually compiles: it didn't in this sandbox,
because the sandbox has no network. GitHub's runners do.

If Android CI fails, it's almost certainly a dependency-version mismatch
(AGP/Kotlin/Compose compiler compatibility) — check the log, it'll name
the exact line. That's normal for a from-scratch scaffold and expected to
need one or two iterations; it's not a sign the architecture is wrong.

## 2. Generate a real signing keystore (one time)

Do this locally — never let the keystore itself touch the repo.

```bash
keytool -genkeypair -v \
  -keystore cairn-release.keystore \
  -alias cairn \
  -keyalg RSA -keysize 2048 -validity 10000
```

You'll be prompted for a keystore password and a key password — remember
both, and remember the alias (`cairn` above). Losing this keystore means
you can never publish an update under the same app identity again, so
back it up somewhere durable (a password manager's file storage, not just
this machine).

## 3. Add the keystore to GitHub as secrets

```bash
base64 -i cairn-release.keystore | pbcopy   # macOS; use `base64 -w0` on Linux
```

In your GitHub repo → **Settings → Secrets and variables → Actions → New
repository secret**, add all four:

| Secret name | Value |
|---|---|
| `CAIRN_KEYSTORE_BASE64` | the base64 output above |
| `CAIRN_KEYSTORE_PASSWORD` | your keystore password |
| `CAIRN_KEY_ALIAS` | `cairn` (or whatever alias you used) |
| `CAIRN_KEY_PASSWORD` | your key password |

Without these, `android-release.yml` still runs and still produces an
installable APK — it just falls back to debug signing, which is fine for
testing but shouldn't be treated as a real production build.

## 4. Cut a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

Pushing a `v*.*.*` tag triggers `.github/workflows/android-release.yml`,
which: runs unit tests, builds `assembleRelease`, signs it with your
keystore secrets (or falls back to debug signing), and attaches the
resulting `.apk` directly to a new **GitHub Release** for that tag —
visible under your repo's **Releases** tab, with a direct download link
anyone can use to sideload it.

## 5. Install it

Download the `.apk` from the Release page on an Android device (or `adb
install app-release.apk` from a computer with the device connected), open
it, allow "install unknown apps" for the browser/file manager if prompted,
and install. First launch walks through permissions and imports existing
call history.

## 6. Deploy the backend (optional)

No separate "artifact" step needed here — Render builds the Docker image
itself directly from your GitHub repo:

1. [render.com](https://render.com) → **New → Blueprint** → connect the
   same GitHub repo → Render finds `backend/render.yaml` automatically.
2. Set the `JWT_SECRET` secret in the Render dashboard (`openssl rand -hex
   32` for a real value).
3. Deploy. Every push to `main` that touches `backend/` triggers a fresh
   Render deploy automatically (that's Render's default GitHub
   integration behavior once connected — no extra workflow needed).
4. Confirm it's alive: `curl https://<your-service>.onrender.com/health`
   should return `{"status":"ok"}`.
5. Point the Android app at it: currently the base URL is a constant in
   `AppModule.kt` (`DEFAULT_BASE_URL`) — update it to your real Render URL
   before cutting a release tag, or wire it to a user-editable setting in
   the Backup screen (marked as a TODO there).

## What "done" looks like

- Actions tab: green checks on `Android CI`, `Backend CI`.
- Releases tab: a tagged release with a downloadable, installable `.apk`.
- Render dashboard: backend service status "Live", `/health` returns 200.

If any of those three aren't true yet, that's the next concrete thing to
fix — not a sign to start over.
