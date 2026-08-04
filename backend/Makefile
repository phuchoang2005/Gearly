# Gearly backend — Docker / Colima setup.
#
# Quick start:
#   make env         # create .env from .env.example (fill in secrets)
#   make up          # build + run the full stack (MongoDB + Spring app)
#   make seed        # load the sample catalog into MongoDB
#   make logs        # tail the app logs
#   make down        # stop everything
#
# `make dev` runs the hot-reload stack (mvn spring-boot:run with a source mount).
# Requires Docker (Colima is fine: `colima start`).

COMPOSE         ?= docker compose
DC_DEV          ?= docker compose -f docker-compose.dev.yml
MONGO_CONTAINER ?= mongodb
MONGO_DB        ?= gearly
SEED_DIR        ?= data/seed
# JDK 21 (Lombok breaks on newer JDKs — see project notes). Override if needed.
JAVA_HOME_21    ?= $(shell /usr/libexec/java_home -v 21 2>/dev/null || echo /usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home)

.DEFAULT_GOAL := help

## ---- Docker stack ----------------------------------------------------------

.PHONY: up
up: env ## Build and start the full stack (MongoDB + Spring app) in the background
	$(COMPOSE) up -d --build

.PHONY: dev
dev: env ## Start the hot-reload dev stack (foreground, mvn spring-boot:run)
	$(DC_DEV) up

.PHONY: build
build: ## Build the app image
	$(COMPOSE) build

.PHONY: down
down: ## Stop the stack (keep data volume)
	-$(COMPOSE) down --remove-orphans
	-$(DC_DEV) down --remove-orphans

.PHONY: clean
clean: ## Stop the stack and delete the MongoDB data volume
	-$(COMPOSE) down -v --remove-orphans
	-$(DC_DEV) down -v --remove-orphans

.PHONY: logs
logs: ## Tail the Spring app logs
	$(COMPOSE) logs -f spring-app

.PHONY: ps
ps: ## Show stack status
	$(COMPOSE) ps

## ---- MongoDB / data --------------------------------------------------------

.PHONY: mongo-up
mongo-up: ## Start just MongoDB and wait for the replica set to elect a primary
	$(COMPOSE) up -d $(MONGO_CONTAINER)
	@printf 'waiting for mongodb replica set'
	@# Not just a ping: the port opens well before rs.initiate() completes, and the app's
	@# first transactional write fails against a set with no primary. The compose
	@# healthcheck does the initiating; this waits for it to report healthy.
	@until docker exec $(MONGO_CONTAINER) mongosh --quiet --eval 'quit(db.hello().isWritablePrimary ? 0 : 1)' >/dev/null 2>&1; do printf '.'; sleep 1; done
	@echo ' primary ready'

.PHONY: rs-status
rs-status: ## Show the replica-set status (rs.status().ok should print 1)
	@docker exec $(MONGO_CONTAINER) mongosh --quiet "mongodb://localhost:27017/$(MONGO_DB)" --eval 'rs.status().ok'

.PHONY: seed
seed: mongo-up ## Seed the sample catalog (data/seed/gearly.*.json) into MongoDB
	@docker exec $(MONGO_CONTAINER) mkdir -p /seed
	@for f in $(SEED_DIR)/gearly.*.json; do docker cp "$$f" $(MONGO_CONTAINER):/seed/$$(basename $$f) >/dev/null; done
	@docker exec $(MONGO_CONTAINER) sh -c 'for f in /seed/gearly.*.json; do \
		coll=$${f#/seed/gearly.}; coll=$${coll%.json}; \
		printf "  %-14s <- %s\n" "$$coll" "$$(basename $$f)"; \
		mongoimport --quiet --uri "mongodb://localhost:27017/$(MONGO_DB)" --collection "$$coll" --file "$$f" --jsonArray --drop; \
	done'
	@echo "seed complete ($(MONGO_DB))"

.PHONY: migrate
migrate: mongo-up ## Apply the in-place Bookify->Gearly schema migration to MongoDB
	@docker exec $(MONGO_CONTAINER) mkdir -p /seed
	@docker cp $(SEED_DIR)/migrate.js $(MONGO_CONTAINER):/seed/migrate.js >/dev/null
	docker exec $(MONGO_CONTAINER) mongosh --quiet "mongodb://localhost:27017/$(MONGO_DB)" /seed/migrate.js

.PHONY: mongosh
mongosh: ## Open a mongosh shell on the Gearly database
	docker exec -it $(MONGO_CONTAINER) mongosh "mongodb://localhost:27017/$(MONGO_DB)"

## ---- Local Maven (no Docker) ----------------------------------------------

.PHONY: compile
compile: ## Compile locally with JDK 21
	JAVA_HOME=$(JAVA_HOME_21) ./mvnw -o compile 2>/dev/null || JAVA_HOME=$(JAVA_HOME_21) mvn -o compile

.PHONY: test
test: ## Run the test suite locally with JDK 21
	JAVA_HOME=$(JAVA_HOME_21) ./mvnw -o test 2>/dev/null || JAVA_HOME=$(JAVA_HOME_21) mvn -o test

.PHONY: run
run: ## Run the app locally with JDK 21 (needs a local MongoDB + env vars)
	JAVA_HOME=$(JAVA_HOME_21) ./mvnw spring-boot:run 2>/dev/null || JAVA_HOME=$(JAVA_HOME_21) mvn spring-boot:run

## ---- Meta ------------------------------------------------------------------

.PHONY: env
env: ## Create .env from .env.example if it does not exist yet
	@if [ ! -f .env ]; then cp .env.example .env && echo "created backend/.env from .env.example — fill in your secrets"; fi

.PHONY: help
help: ## Show this help
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'
