# Movie Matcher - Local Testing Guide

This guide will help you test the application locally before deploying with Docker.

## Prerequisites

- Rust (install from https://rustup.rs)
- Node.js 20+ (install from https://nodejs.org)
- OMDB API Key (get free key from https://www.omdbapi.com/apikey.aspx)

## Step 1: Test Backend

### 1.1 Set up environment

```bash
cd backend

# Create .env file
cat > .env << EOF
OMDB_API_KEY=your_actual_api_key_here
RUST_LOG=info
EOF
```

**IMPORTANT:** Replace `your_actual_api_key_here` with your real OMDB API key!

### 1.2 Build and run backend

```bash
# Build (first time will take a few minutes)
cargo build --release

# Run the server
cargo run --release
```

You should see:
```
OMDB API key loaded successfully
Starting server on 0.0.0.0:3000
```

### 1.3 Test backend (in a new terminal)

```bash
# Test health endpoint
curl http://localhost:3000/health

# Expected response:
# {"service":"movie-matcher-backend","status":"ok"}

# Create a test room
curl -X POST http://localhost:3000/api/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "host_id": "TestUser",
    "filters": {
      "genre": "Action",
      "year_from": 2000,
      "year_to": 2020
    }
  }'

# Expected response (room_id will be different):
# {"room_id":"abc-123-def","join_url":"/room/abc-123-def"}

# Get room info (replace ROOM_ID with the one from above)
curl http://localhost:3000/api/rooms/ROOM_ID

# Expected response:
# {"id":"ROOM_ID","filters":{"genre":"Action","year_from":2000,"year_to":2020},"participants_count":1,"is_active":false}
```

If all these work, backend is good! ✅

## Step 2: Test Frontend

### 2.1 Install dependencies

```bash
cd frontend
npm install
```

### 2.2 Run development server

```bash
npm run dev
```

You should see:
```
  VITE v5.x.x  ready in xxx ms

  ➜  Local:   http://localhost:8080/
  ➜  Network: use --host to expose
```

### 2.3 Test in browser

1. Open http://localhost:8080 in your browser
2. You should see "Movie Matcher" with a form to create a room

## Step 3: Test Full Flow

**Keep both backend and frontend running in separate terminals!**

### 3.1 Create a room

1. In your browser at http://localhost:8080
2. Enter your name (e.g., "Alice")
3. Select a genre (optional, e.g., "Action")
4. Enter year range (optional, e.g., 2010 to 2020)
5. Click "Create Room"

You should be redirected to the room page with a shareable link.

### 3.2 Join the room (simulate another user)

1. Open a new **incognito/private browser window**
2. Copy the room URL from the first window (e.g., http://localhost:8080/room/abc-123)
3. Paste it in the incognito window
4. Enter a different name (e.g., "Bob")
5. Click "Join Room"

Both windows should now show 2 participants.

### 3.3 Start matching

1. In the first window (host), click "Start Matching"
2. Both windows should show movies appearing every 3 seconds
3. You should see movie posters, titles, years, genres, ratings, and plots

If you see movies appearing, everything works! 🎉

## Troubleshooting

### Backend Issues

**Error: "OMDB_API_KEY must be set"**
- Make sure you created `backend/.env` with a valid API key

**Error: "Failed to bind address"**
- Port 3000 is already in use
- Find and kill the process: `lsof -ti:3000 | xargs kill -9`

**Error when fetching movies**
- Check your API key is valid
- Free tier has rate limits (1,000 requests/day)
- Try different search filters

### Frontend Issues

**Error: "Cannot connect to backend"**
- Make sure backend is running on port 3000
- Check browser console for errors

**WebSocket connection fails**
- Backend must be running
- Check firewall isn't blocking the connection

**Movies not appearing**
- Check browser console for errors
- Verify backend logs for OMDB API errors
- Try broader filters (remove genre, widen year range)

## Next Steps

Once local testing works:
1. Stop both servers (Ctrl+C)
2. Let me know it works
3. I'll rebuild the Docker files properly
4. Then we'll test with Docker
5. Finally deploy to Kubernetes

## Manual Stop

To stop the servers:
- Press `Ctrl+C` in each terminal window
- If needed, kill processes:
  ```bash
  # Kill backend
  lsof -ti:3000 | xargs kill -9
  
  # Kill frontend
  lsof -ti:8080 | xargs kill -9
  ```
