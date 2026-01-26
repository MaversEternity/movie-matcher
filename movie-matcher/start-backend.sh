#!/bin/bash

cd backend

if [ ! -f .env ]; then
    echo "❌ Error: backend/.env not found"
    echo "Please create it with your OMDB_API_KEY"
    echo ""
    echo "Example:"
    echo "  cd backend"
    echo "  echo 'OMDB_API_KEY=your_key_here' > .env"
    echo "  echo 'RUST_LOG=info' >> .env"
    exit 1
fi

source .env

if [ -z "$OMDB_API_KEY" ] || [ "$OMDB_API_KEY" = "your_actual_api_key_here" ] || [ "$OMDB_API_KEY" = "your_test_key_here" ]; then
    echo "❌ Error: Please set a valid OMDB_API_KEY in backend/.env"
    echo "Get your free API key at: https://www.omdbapi.com/apikey.aspx"
    exit 1
fi

echo "🚀 Starting Movie Matcher Backend..."
echo "Building..."
cargo build --release

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo "Starting server on http://localhost:3000"
    echo "Press Ctrl+C to stop"
    echo ""
    cargo run --release
else
    echo "❌ Build failed"
    exit 1
fi
