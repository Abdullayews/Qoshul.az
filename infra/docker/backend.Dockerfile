# STUB — Mərhələ 2-də real multi-stage build ilə əvəz olunacaq
# (npm ci --omit=dev, distroless/alpine non-root istifadəçi, healthcheck).
#
# Baza: node:24-alpine — linux/arm64 təsdiqlənib (docs/decisions.md D-006).
# Node 24: Maintenance LTS (2026-10-20-dən), EOL 2028-04-30 (D-007).
FROM node:24-alpine

WORKDIR /app

CMD ["echo", "Qoshul backend — mərhələ 2-də gəlir"]
