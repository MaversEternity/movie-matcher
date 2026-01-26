#!/bin/bash

echo "🎬 Movie Matcher - Local Testing"
echo "================================"
echo ""

# Check if API key is set
if [ ! -f backend/.env ]; then
    echo "❌ Error: backend/.env not found"
    echo "Please create backend/.env with your OMDB_API_KEY"
    exit 1
fi

# Start backend in background
echo "Starting backend..."
cd backend
RUST_LOG=info cargo run --release &
BACKEND_PID=$!
cd ..

echo "Backend started (PID: $BACKEND_PID)"
echo "Waiting for backend to be ready..."
sleep 3

# Test backend health
echo "Testing backend health..."
HEALTH=$(curl -s http://localhost:3000/health 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "✅ Backend is healthy: $HEALTH"
else
    echo "❌ Backend health check failed"
    kill $BACKEND_PID 2>/dev/null
    exit 1
fi

echo ""
echo "Starting frontend..."
cd frontend
npm run dev &
FRONTEND_PID=$!
cd ..

echo ""
echo "================================"
echo "✅ Application is running!"
echo ""
echo "Frontend: http://localhost:8080"
echo "Backend:  http://localhost:3000"
echo ""
echo "Press Ctrl+C to stop all services"
echo "================================"
echo ""

# Handle Ctrl+C
trap "echo ''; echo 'Stopping services...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" INT

# Wait for processes
wait
