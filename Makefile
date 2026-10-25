# Qoshul.az — inkişaf və deploy əmrləri
# Qayda: bütün host faktları .env-də (runbook.md §3) — fayllar host-agnostik.
SHELL := /bin/bash
COMPOSE := docker compose --env-file .env

.DEFAULT_GOAL := help
.PHONY: help dev down ps logs vm1-up vm1-down vm2-up vm2-down

help: ## Bu köməyi göstər
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-10s %s\n", $$1, $$2}'

dev: ## Lokal dev: PG + PostGIS + Redis qaldır (build daxil)
	$(COMPOSE) -f infra/compose/docker-compose.dev.yml up -d --build

down: ## Lokal dev-i dayandır
	$(COMPOSE) -f infra/compose/docker-compose.dev.yml down

ps: ## Dev konteyner vəziyyəti
	$(COMPOSE) -f infra/compose/docker-compose.dev.yml ps

logs: ## Dev logları (follow)
	$(COMPOSE) -f infra/compose/docker-compose.dev.yml logs -f

vm1-up: ## VM1 (prod): Caddy + backend + Redis
	$(COMPOSE) -f infra/compose/docker-compose.vm1.yml up -d

vm1-down: ## VM1-i dayandır
	$(COMPOSE) -f infra/compose/docker-compose.vm1.yml down

vm2-up: ## VM2 (prod): PostgreSQL + PostGIS (ilk dəfə build ilə)
	$(COMPOSE) -f infra/compose/docker-compose.vm2.yml up -d --build

vm2-down: ## VM2-ni dayandır
	$(COMPOSE) -f infra/compose/docker-compose.vm2.yml down
