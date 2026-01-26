#!/bin/bash

cd frontend

if [ ! -d node_modules ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

echo "🚀 Starting Movie Matcher Frontend..."
echo "Development server will start on http://localhost:8080"
echo "Press Ctrl+C to stop"
echo ""
npm run dev
