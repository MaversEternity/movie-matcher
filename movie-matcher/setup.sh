#!/bin/bash

echo "🎬 Movie Matcher Setup Script"
echo "=============================="
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "Creating .env file from template..."
    cp .env.example .env
    echo ""
    echo "⚠️  IMPORTANT: Please edit .env and add your OMDB API key!"
    echo "Get your free API key at: https://www.omdbapi.com/apikey.aspx"
    echo ""
    read -p "Press Enter after you've added your API key to .env..."
fi

# Verify API key is set
source .env
if [ -z "$OMDB_API_KEY" ] || [ "$OMDB_API_KEY" = "your_api_key_here" ]; then
    echo "❌ Error: OMDB_API_KEY is not set in .env file"
    exit 1
fi

echo "✅ Environment configured"
echo ""

echo "Building and starting containers..."
docker-compose up --build -d

echo ""
echo "✅ Services started successfully!"
echo ""
echo "Access the application at:"
echo "  Frontend: http://localhost:8080"
echo "  Backend:  http://localhost:3000"
echo ""
echo "To view logs: docker-compose logs -f"
echo "To stop:      docker-compose down"
echo ""
echo "Enjoy matching movies with your friends! 🍿"
