.PHONY: help dev up down clean logs test build

help:
	@echo "🧠 MindForge - Available commands (Run from setup/ folder):"
	@echo ""
	@echo "  make dev       - Start DB + Ollama (for local development)"
	@echo "  make up        - Start all services (DB + Ollama + API)"
	@echo "  make down      - Stop all services"
	@echo "  make clean     - Stop and remove all containers + volumes"
	@echo "  make logs      - Show logs from all services"
	@echo "  make test      - Run tests"
	@echo "  make build     - Build API jar"
	@echo "  make setup-ai  - Download Ollama models"
	@echo ""

# Modo dev: só DB + Ollama (API roda na IDE)
dev:
	@echo "🚀 Starting dev environment (DB + Ollama)..."
	docker-compose up -d postgres ollama
	@echo "⏳ Waiting for services to be ready..."
	@sleep 5
	@echo "✅ Dev environment ready!"
	@echo ""
	@echo "📊 Database:  postgresql://localhost:5432/mindforge"
	@echo "🤖 Ollama:    http://localhost:11434"
	@echo ""
	@echo "💡 Run 'make setup-ai' to download AI models"
	@echo "💡 Then start API: ../mvnw spring-boot:run"

# Todos os serviços (incluindo API)
up:
	@echo "🚀 Starting full stack..."
	docker-compose up -d
	@echo "⏳ Waiting for services..."
	@sleep 10
	@echo "✅ All services ready!"
	@echo ""
	@echo "🌐 API:       http://localhost:8080"
	@echo "📖 Swagger:  http://localhost:8080/swagger-ui.html"
	@echo "📊 Database: postgresql://localhost:5432/mindforge"
	@echo "🤖 Ollama:   http://localhost:11434"

# Para tudo
down:
	@echo "🛑 Stopping all services..."
	docker-compose down
	@echo "✅ All services stopped"

# Limpa tudo (containers + volumes)
clean:
	@echo "🧹 Cleaning up..."
	docker-compose down -v
	@echo "✅ All containers and volumes removed"

# Logs
logs:
	docker-compose logs -f

# Testes
test:
	@echo "🧪 Running tests..."
	../mvnw test

# Build
build:
	@echo "🔨 Building API..."
	../mvnw clean package -DskipTests
	@echo "✅ Build complete: target/*.jar"

# Setup AI models
setup-ai:
	@bash setup-ollama.sh
