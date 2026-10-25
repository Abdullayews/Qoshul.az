# Monitoring (Mərhələ 10)

- **Prometheus** — backend metrics (NestJS /metrics) + node_exporter (VM1, VM2)
- **Grafana** — dashboard-lar; kritik alert: hər hansı VM-də yaddaş < 25% (idle-reclaim qorunması — runbook §5)
- **Sentry** — backend (DSN: `SENTRY_DSN_BACKEND`) + Flutter (`SENTRY_DSN_FLUTTER`)

Konfiq faylları bu qovluğa m10-da gəlir. Yaddaş büdcəsi: runbook §5.
