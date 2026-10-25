# CI/CD (Mərhələ 10)

GitHub Actions workflow-ları:

1. **PR:** lint → Vitest → build (backend + Flutter analyze)
2. **main merge:** multi-arch Docker build (`linux/amd64` + `linux/arm64` — A1 üçün məcburi) → GHCR-ə push:
   - `qoshul-backend` (infra/docker/backend.Dockerfile)
   - `qoshul-postgres` (infra/docker/postgres/Dockerfile)
3. **Deploy:** VM1/VM2-yə SSH ilə `git pull` + `docker compose pull` + `up -d`

Secrets (qayda 3): `GHCR_TOKEN`, `SSH_PRIVATE_KEY_VM1`, `SSH_PRIVATE_KEY_VM2`, `SENTRY_DSN_BACKEND`.

Workflow faylları bu qovluğa m10-da gəlir.
