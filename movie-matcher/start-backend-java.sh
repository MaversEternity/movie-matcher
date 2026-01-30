#!/bin/bash

cd backend-java

if [ ! -f .env ]; then
    echo "❌ Error: backend-java/.env not found"
    echo "Please create it with your OMDB_API_KEY"
    echo ""
    echo "Example:"
    echo "  cd backend-java"
    echo "  echo 'OMDB_API_KEY=your_key_here' > .env"
    exit 1
fi

source .env

if [ -z "$OMDB_API_KEY" ] || [ "$OMDB_API_KEY" = "your_actual_api_key_here" ] || [ "$OMDB_API_KEY" = "your_test_key_here" ]; then
    echo "❌ Error: Please set a valid OMDB_API_KEY in backend-java/.env"
    echo "Get your free API key at: https://www.omdbapi.com/apikey.aspx"
    exit 1
fi

echo "🚀 Starting Movie Matcher Backend (Java)..."
echo ""

# Check if system Maven is available and has the right version
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn --version | head -n 1 | cut -d ' ' -f 3)
    echo "Found system Maven version: $MVN_VERSION"
    MAVEN_CMD="mvn"
elif [ -f ./mvnw ]; then
    echo "Using Maven wrapper..."
    MAVEN_CMD="./mvnw"
else
    echo "❌ Error: Maven not found"
    echo "Please install Maven: brew install maven"
    exit 1
fi

echo "Starting Quarkus in dev mode..."
echo "Server will start on http://localhost:3000"
echo "Press Ctrl+C to stop"
echo ""
echo "Health check: http://localhost:3000/q/health"
echo "Metrics: http://localhost:3000/q/metrics"
echo ""

export OMDB_API_KEY=$OMDB_API_KEY
$MAVEN_CMD quarkus:dev
