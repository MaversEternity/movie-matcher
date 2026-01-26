# Movie Matcher

A real-time movie matching application for groups of friends. Create a room, share the link, and discover movies together!

## Features

- **Room-based matching**: Host creates a room with custom filters
- **Real-time updates**: WebSocket-based live movie streaming
- **OMDB Integration**: Fetch movies with ratings, plots, and posters
- **Filters**: Genre and year range filtering
- **Production-ready**: Built with Rust + Axum backend and Vue.js frontend
- **Containerized**: Docker and Kubernetes ready

## Architecture

### Backend (Rust)
- **Framework**: Axum (modern async web framework)
- **WebSocket**: Real-time bidirectional communication
- **API Integration**: OMDB API for movie data
- **Concurrency**: Built on Tokio runtime

### Frontend (Vue.js)
- **Framework**: Vue 3 with Composition API
- **Build Tool**: Vite for fast development
- **Styling**: Scoped CSS with modern gradients

## Quick Start

### Prerequisites

- Docker & Docker Compose
- OMDB API Key (get free key at: https://www.omdbapi.com/apikey.aspx)

### Using Docker Compose

1. Clone the repository and navigate to the project:
```bash
cd movie-matcher
```

2. Create a `.env` file in the root directory:
```bash
echo "OMDB_API_KEY=your_api_key_here" > .env
```

3. Build and run:
```bash
docker-compose up --build
```

4. Access the application:
- Frontend: http://localhost:8080
- Backend API: http://localhost:3000

## Development Setup

### Backend Development

1. Navigate to backend directory:
```bash
cd backend
```

2. Create `.env` file:
```bash
cp .env.example .env
# Edit .env and add your OMDB_API_KEY
```

3. Run the backend:
```bash
cargo run
```

The backend will start on `http://localhost:3000`

### Frontend Development

1. Navigate to frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Run development server:
```bash
npm run dev
```

The frontend will start on `http://localhost:8080`

## Kubernetes Deployment

### Prerequisites
- Kubernetes cluster
- kubectl configured
- Docker images built and pushed to registry

### Build Docker Images

```bash
# Build backend
cd backend
docker build -t your-registry/movie-matcher-backend:latest .
docker push your-registry/movie-matcher-backend:latest

# Build frontend
cd ../frontend
docker build -t your-registry/movie-matcher-frontend:latest .
docker push your-registry/movie-matcher-frontend:latest
```

### Deploy to Kubernetes

1. Update image references in deployment files:
```bash
# Edit k8s/backend-deployment.yaml and k8s/frontend-deployment.yaml
# Change image: movie-matcher-backend:latest
# To: image: your-registry/movie-matcher-backend:latest
```

2. Create secret with your OMDB API key:
```bash
kubectl create secret generic movie-matcher-secrets \
  --from-literal=omdb-api-key=YOUR_API_KEY \
  -n movie-matcher
```

Or edit `k8s/secret.yaml` and apply:
```bash
kubectl apply -f k8s/secret.yaml
```

3. Apply all manifests:
```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/ingress.yaml
```

4. Verify deployment:
```bash
kubectl get pods -n movie-matcher
kubectl get services -n movie-matcher
kubectl get ingress -n movie-matcher
```

### Configure Ingress

Edit `k8s/ingress.yaml` to match your domain and ingress controller:
- Update `host` to your domain
- Configure TLS if needed
- Adjust annotations for your ingress controller

## API Endpoints

### Backend API

- `GET /health` - Health check
- `POST /api/rooms` - Create a new room
- `GET /api/rooms/:room_id` - Get room info
- `POST /api/rooms/:room_id/join` - Join a room
- `POST /api/rooms/:room_id/start` - Start matching (host only)
- `GET /api/rooms/:room_id/ws` - WebSocket connection

### WebSocket Messages

Server messages:
- `ParticipantJoined` - New participant joined
- `ParticipantLeft` - Participant left
- `MatchingStarted` - Matching session started
- `NewMovie` - New movie to display
- `MatchingEnded` - All movies shown
- `Error` - Error occurred

## Environment Variables

### Backend
- `OMDB_API_KEY` (required) - Your OMDB API key
- `RUST_LOG` (optional) - Log level (default: info)

### Frontend
- `VITE_API_URL` (optional) - Backend API URL for development

## Security Features

- API key stored securely in environment variables
- Kubernetes secrets for sensitive data
- CORS configured for frontend access
- WebSocket authentication via room ID
- No API key exposed to frontend

## Production Considerations

1. **API Rate Limiting**: OMDB free tier has rate limits
2. **Room Cleanup**: Implement TTL for inactive rooms
3. **Scaling**: Backend is stateless (except WebSocket connections)
4. **Monitoring**: Add metrics and health checks
5. **Logging**: Configure log aggregation
6. **TLS**: Enable HTTPS in production

## Troubleshooting

### Backend won't start
- Verify OMDB_API_KEY is set
- Check port 3000 is available
- Review logs: `docker-compose logs backend`

### Frontend can't connect to backend
- Check CORS settings
- Verify backend is running
- Check network connectivity
- Review browser console for errors

### WebSocket connection fails
- Ensure WebSocket support in reverse proxy
- Check firewall rules
- Verify room exists before connecting

### No movies appearing
- Verify OMDB API key is valid
- Check API rate limits
- Review backend logs for API errors
- Try different filter combinations

## License

MIT
