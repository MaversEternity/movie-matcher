.PHONY: help build up down logs clean test deploy-k8s

help:
	@echo "Movie Matcher - Available commands:"
	@echo "  make build        - Build Docker images"
	@echo "  make up           - Start services with docker-compose"
	@echo "  make down         - Stop services"
	@echo "  make logs         - View logs"
	@echo "  make clean        - Clean up containers and images"
	@echo "  make test         - Run tests"
	@echo "  make deploy-k8s   - Deploy to Kubernetes"

build:
	docker-compose build

up:
	docker-compose up -d
	@echo "Services started!"
	@echo "Frontend: http://localhost:8080"
	@echo "Backend: http://localhost:3000"

down:
	docker-compose down

logs:
	docker-compose logs -f

clean:
	docker-compose down -v
	docker system prune -f

test:
	@echo "Running backend tests..."
	cd backend && cargo test
	@echo "Backend tests complete!"

deploy-k8s:
	@echo "Deploying to Kubernetes..."
	kubectl apply -f k8s/namespace.yaml
	kubectl apply -f k8s/secret.yaml
	kubectl apply -f k8s/backend-deployment.yaml
	kubectl apply -f k8s/frontend-deployment.yaml
	kubectl apply -f k8s/ingress.yaml
	@echo "Deployment complete!"
	@echo "Check status with: kubectl get pods -n movie-matcher"
