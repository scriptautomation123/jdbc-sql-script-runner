#!/bin/sh

# Check if Docker is installed
if ! command -v docker >/dev/null 2>&1; then
    echo "Error: Docker is not installed. Please install Docker before continuing."
    exit 1
fi

# Check if Docker Compose is available (either as docker compose or docker-compose)
if ! (docker compose version >/dev/null 2>&1 || command -v docker-compose >/dev/null 2>&1); then
    echo "Error: Docker Compose is not available. Please ensure Docker is up to date or install Docker Compose separately."
    exit 1
fi

# Check if VS Code is installed
if ! command -v code >/dev/null 2>&1; then
    echo "Warning: VS Code CLI is not installed. Some features might not work properly."
fi

# Check if Git is installed
if ! command -v git >/dev/null 2>&1; then
    echo "Error: Git is not installed. Please install Git before continuing."
    exit 1
fi

echo "All basic requirements are met. Proceeding with container creation..." 