# Quick Start - Local Testing

## Step 1: Get OMDB API Key

1. Go to https://www.omdbapi.com/apikey.aspx
2. Select "FREE! (1,000 daily limit)"
3. Enter your email
4. Check your email and activate the key
5. Copy your API key

## Step 2: Configure Backend

```bash
cd backend

# Create .env file with your API key
echo "OMDB_API_KEY=YOUR_KEY_HERE" > .env
echo "RUST_LOG=info" >> .env
```

## Step 3: Start Backend

**Terminal 1:**
```bash
./start-backend.sh
```

Wait for: `Starting server on 0.0.0.0:3000`

## Step 4: Start Frontend

**Terminal 2:**
```bash
./start-frontend.sh
```

Wait for: `Local: http://localhost:8080/`

## Step 5: Test the App

1. Open http://localhost:8080 in your browser
2. Create a room with your name and optional filters
3. Copy the room link
4. Open the link in a private/incognito window
5. Join with a different name
6. Click "Start Matching" from the host window
7. Watch movies appear!

## That's it!

Once you confirm everything works, let me know and I'll fix the Docker setup.

## Troubleshooting

**Port already in use:**
```bash
# Kill process on port 3000
lsof -ti:3000 | xargs kill -9

# Kill process on port 8080  
lsof -ti:8080 | xargs kill -9
```

**No movies appearing:**
- Check your API key is valid
- Try removing genre filter
- Check backend terminal for errors
