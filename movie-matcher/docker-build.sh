#!/bin/bash
set -e

echo "Building Docker images..."
docker-compose build

echo ""
echo "Docker images built successfully!"
echo ""
echo "To start the application, run:"
echo "  docker-compose up -d"
echo ""
echo "To stop the application, run:"
echo "  docker-compose down"
