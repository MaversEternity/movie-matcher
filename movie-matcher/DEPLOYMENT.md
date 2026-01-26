# Movie Matcher - Deployment Guide

## Quick Start (Docker Compose)

The fastest way to get started:

```bash
# 1. Get your OMDB API key from https://www.omdbapi.com/apikey.aspx

# 2. Run the setup script
./setup.sh

# Or manually:
cp .env.example .env
# Edit .env and add your API key
docker-compose up --build
```

Access at http://localhost:8080

## Kubernetes Deployment

### Prerequisites
- Kubernetes cluster running
- kubectl configured
- Container registry access (or use local images with kind/minikube)

### Step 1: Build and Push Images

```bash
# Build backend
cd backend
docker build -t your-registry.com/movie-matcher-backend:v1.0.0 .
docker push your-registry.com/movie-matcher-backend:v1.0.0

# Build frontend  
cd ../frontend
docker build -t your-registry.com/movie-matcher-frontend:v1.0.0 .
docker push your-registry.com/movie-matcher-frontend:v1.0.0
```

### Step 2: Update Kubernetes Manifests

Edit `k8s/backend-deployment.yaml` and `k8s/frontend-deployment.yaml`:

```yaml
# Change from:
image: movie-matcher-backend:latest

# To:
image: your-registry.com/movie-matcher-backend:v1.0.0
```

### Step 3: Create Secret

**Option A: Using kubectl**
```bash
kubectl create secret generic movie-matcher-secrets \
  --from-literal=omdb-api-key=YOUR_ACTUAL_API_KEY \
  -n movie-matcher
```

**Option B: Using manifest**
```bash
# Edit k8s/secret.yaml and replace 'your_api_key_here' with actual key
kubectl apply -f k8s/secret.yaml
```

### Step 4: Deploy Application

```bash
# Apply all manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl apply -f k8s/ingress.yaml

# Or use the Makefile
make deploy-k8s
```

### Step 5: Configure Ingress

Edit `k8s/ingress.yaml` to match your setup:

```yaml
spec:
  ingressClassName: nginx  # or traefik, etc.
  rules:
  - host: movies.yourdomain.com  # Your actual domain
```

For HTTPS, uncomment the TLS section:
```yaml
tls:
- hosts:
  - movies.yourdomain.com
  secretName: movie-matcher-tls
```

### Step 6: Verify Deployment

```bash
# Check pods are running
kubectl get pods -n movie-matcher

# Check services
kubectl get svc -n movie-matcher

# Check ingress
kubectl get ingress -n movie-matcher

# View logs
kubectl logs -f deployment/movie-matcher-backend -n movie-matcher
kubectl logs -f deployment/movie-matcher-frontend -n movie-matcher
```

## Production Considerations

### Security
- [ ] Use Kubernetes secrets for API keys (never commit them)
- [ ] Enable TLS/HTTPS in production
- [ ] Configure network policies
- [ ] Use non-root containers
- [ ] Scan images for vulnerabilities

### Scaling
- [ ] Adjust replica counts based on load
- [ ] Configure HPA (Horizontal Pod Autoscaler)
- [ ] Backend is stateless (except WebSocket connections)
- [ ] Use load balancer with session affinity for WebSockets

### Monitoring
- [ ] Add Prometheus metrics
- [ ] Configure health check endpoints
- [ ] Set up logging aggregation (ELK, Loki)
- [ ] Create alerts for errors and downtime

### Performance
- [ ] Configure resource requests/limits appropriately
- [ ] Enable compression in nginx
- [ ] Cache static assets
- [ ] Consider CDN for frontend assets

### Resilience
- [ ] Configure pod disruption budgets
- [ ] Set up backup and disaster recovery
- [ ] Implement rate limiting
- [ ] Add circuit breakers for OMDB API

## Environment-Specific Configs

### Development
```yaml
replicas: 1
resources:
  requests:
    memory: "64Mi"
    cpu: "50m"
```

### Staging
```yaml
replicas: 2
resources:
  requests:
    memory: "128Mi"
    cpu: "100m"
```

### Production
```yaml
replicas: 3
resources:
  requests:
    memory: "256Mi"
    cpu: "200m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

## Troubleshooting

### Pods not starting
```bash
# Check events
kubectl describe pod <pod-name> -n movie-matcher

# Check logs
kubectl logs <pod-name> -n movie-matcher

# Common issues:
# - Missing API key secret
# - Image pull errors
# - Insufficient resources
```

### Backend health check failing
```bash
# Test directly
kubectl port-forward svc/movie-matcher-backend 3000:3000 -n movie-matcher
curl http://localhost:3000/health
```

### WebSocket connections failing
```bash
# Check ingress annotations for WebSocket support
# Ensure timeout values are high enough
# Verify backend pods are healthy
```

### Can't access application
```bash
# Check ingress
kubectl get ingress -n movie-matcher
kubectl describe ingress movie-matcher-ingress -n movie-matcher

# Verify DNS is pointing to your ingress controller
# Check that ingress controller is installed
```

## Rollback

```bash
# View deployment history
kubectl rollout history deployment/movie-matcher-backend -n movie-matcher

# Rollback to previous version
kubectl rollout undo deployment/movie-matcher-backend -n movie-matcher

# Rollback to specific revision
kubectl rollout undo deployment/movie-matcher-backend --to-revision=2 -n movie-matcher
```

## Cleanup

```bash
# Remove all resources
kubectl delete namespace movie-matcher

# Or individually
kubectl delete -f k8s/
```

## CI/CD Integration

Example GitHub Actions workflow:

```yaml
name: Deploy to Kubernetes

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Build and Push Backend
        run: |
          docker build -t ${{ secrets.REGISTRY }}/movie-matcher-backend:${{ github.sha }} ./backend
          docker push ${{ secrets.REGISTRY }}/movie-matcher-backend:${{ github.sha }}
      
      - name: Build and Push Frontend
        run: |
          docker build -t ${{ secrets.REGISTRY }}/movie-matcher-frontend:${{ github.sha }} ./frontend
          docker push ${{ secrets.REGISTRY }}/movie-matcher-frontend:${{ github.sha }}
      
      - name: Deploy to Kubernetes
        run: |
          kubectl set image deployment/movie-matcher-backend \
            backend=${{ secrets.REGISTRY }}/movie-matcher-backend:${{ github.sha }} \
            -n movie-matcher
```

## Support

For issues and questions:
- Check logs: `kubectl logs -f deployment/movie-matcher-backend -n movie-matcher`
- Review the main README.md
- Check OMDB API status: https://www.omdbapi.com
