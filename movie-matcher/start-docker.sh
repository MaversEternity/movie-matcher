#!/bin/bash

echo "🚀 Starting Movie Matcher with Docker"
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "⚠️  .env file not found. Creating from .env.example..."
    if [ -f .env.example ]; then
        cp .env.example .env
        echo "✅ Created .env file. Please edit it and add your OMDB_API_KEY"
        echo ""
    fi
fi

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop and try again."
    exit 1
fi

echo "📦 Building and starting containers..."
echo ""

# Start services
docker-compose up -d --build

# Wait for services to be healthy
echo ""
echo "⏳ Waiting for services to be ready..."
echo ""

# Wait for PostgreSQL
echo "Checking PostgreSQL..."
until docker-compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; do
    echo -n "."
    sleep 1
done
echo " ✅ PostgreSQL is ready"

# Wait for Backend
echo "Checking Backend..."
max_attempts=30
attempt=0
until curl -f http://localhost:3000/health > /dev/null 2>&1; do
    if [ $attempt -eq $max_attempts ]; then
        echo " ❌ Backend failed to start after ${max_attempts} seconds"
        echo ""
        echo "Logs:"
        docker-compose logs backend-java
        exit 1
    fi
    echo -n "."
    sleep 2
    ((attempt++))
done
echo " ✅ Backend is ready"

echo ""
echo "✅ All services are running!"
echo ""
echo "📌 Access points:"
echo "   Backend API:    http://localhost:3000"
echo "   Health Check:   http://localhost:3000/health"
echo "   PostgreSQL:     localhost:5432"
echo ""
echo "💡 Useful commands:"
echo "   View logs:      docker-compose logs -f"
echo "   Stop services:  docker-compose stop"
echo "   Restart:        docker-compose restart"
echo ""
echo "🎬 To start the frontend, run in another terminal:"
echo "   cd frontend && npm run dev"
echo ""
