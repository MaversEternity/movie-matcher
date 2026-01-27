# --- Stage 1: Builder ---
FROM rust:1.83-slim AS builder
WORKDIR /usr/src/app

# Install build dependencies
RUN apt-get update && apt-get install -y pkg-config libssl-dev && rm -rf /var/lib/apt/lists/*

COPY Cargo.toml Cargo.lock ./
RUN mkdir src/ && echo "fn main() {}" > src/main.rs && cargo build --release && rm -rf src
COPY src src
RUN cargo build --release

# --- Stage 2: Runtime ---
FROM debian:bookworm-slim AS final
RUN apt-get update && apt-get install -y ca-certificates libssl3 && rm -rf /var/lib/apt/lists/*
WORKDIR /usr/local/bin
COPY --from=builder /usr/src/app/target/release/movie-matcher-backend ./app
EXPOSE 3000
CMD ["./app"]
