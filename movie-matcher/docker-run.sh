#!/bin/bash
set -e

# Check if .env exists
if [ ! -f .env ]; then
    echo "Error: .env file not found!"
    echo "Please create .env file with your OMDB_API_KEY"
    echo "Example: OMDB_API_KEY=your_key_here"
    exit 1
fi

echo "Starting Movie Matcher application..."
docker-compose up -d

echo ""
echo "Application started!"
echo ""
echo "Frontend: http://localhost:8080"
echo "Backend API: http://localhost:3000"
echo ""
echo "To view logs:"
echo "  docker-compose logs -f"
echo ""
echo "To stop:"
echo "  docker-compose down"
