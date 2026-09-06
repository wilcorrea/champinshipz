PORT   ?= 8080
GRADLE := ./gradlew
BASE   := http://localhost:$(PORT)
LOG    := build/api.log
DOCS   := docs/openapi.yaml

.DEFAULT_GOAL := help
.PHONY: help run dev stop status logs test build openapi mongo mongo-stop clean

help: ## mostra esta ajuda
	@grep -E '^[a-z][a-z-]*:.*## ' $(MAKEFILE_LIST) \
		| sed 's/:.*## /|/' \
		| awk -F'|' '{printf "  \033[36m%-11s\033[0m %s\n", $$1, $$2}'

run: mongo ## sobe a API em primeiro plano (Ctrl-C encerra)
	$(GRADLE) bootRun --console=plain --args='--server.port=$(PORT)'

dev: mongo stop ## sobe a API em segundo plano, log em build/api.log
	@mkdir -p build
	@nohup $(GRADLE) bootRun --console=plain --args='--server.port=$(PORT)' > $(LOG) 2>&1 &
	@echo "subindo em $(BASE) — acompanhe com: make logs"

stop: ## encerra quem estiver ocupando a porta
	@pids=$$(lsof -ti tcp:$(PORT) 2>/dev/null); \
	if [ -z "$$pids" ]; then \
		echo "nada escutando em $(PORT)"; \
	else \
		kill $$pids; \
		for i in 1 2 3 4 5 6 7 8 9 10; do \
			lsof -ti tcp:$(PORT) >/dev/null 2>&1 || break; \
			sleep 1; \
		done; \
		echo "encerrei" $$pids; \
	fi

status: ## checa se a API responde
	@code=$$(curl -fs -o /dev/null -w '%{http_code}' $(BASE)/api/championships 2>/dev/null); \
	if [ "$$code" = "200" ]; then \
		echo "API no ar em $(BASE)"; \
	else \
		echo "API fora do ar em $(BASE)"; \
	fi

logs: ## acompanha o log do modo dev
	@tail -f $(LOG)

test: ## roda os testes
	$(GRADLE) test

build: ## gera o jar em build/libs
	$(GRADLE) bootJar

openapi: ## reexporta docs/openapi.yaml a partir da API no ar
	@curl -fsS $(BASE)/v3/api-docs.yaml -o $(DOCS) \
		&& echo "gravado em $(DOCS)" \
		|| echo "a API precisa estar no ar — rode make dev antes"

mongo: ## garante o mongo do docker-compose no ar
	@lsof -ti tcp:27017 >/dev/null 2>&1 || docker compose up -d mongo

mongo-stop: ## para o mongo
	docker compose stop mongo

clean: ## limpa build/
	$(GRADLE) clean
