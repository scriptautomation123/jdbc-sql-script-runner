#!/bin/sh

echo "Setting up development environment..."

# Configure Git with a container-specific config to avoid modifying global settings
git config --local credential.helper store

# Setup GitHub CLI authentication only if user has write access
if gh auth status 2>/dev/null; then
    echo "Configuring GitHub CLI..."
    gh auth setup-git
else
    echo "Note: GitHub CLI authentication skipped (read-only access is fine)"
fi

# Install Java dependencies if needed (using a local Maven repository)
if [ -f "pom.xml" ]; then
    echo "Installing Maven dependencies..."
    # Use a container-local Maven repository
    mvn install -DskipTests -Dmaven.repo.local=/workspaces/.m2/repository
fi

# Handle environment setup without modifying workspace
if [ ! -f ".env" ]; then
    echo "Note: Using default environment settings for read-only mode"
    # Load environment variables from example without creating a file
    if [ -f ".env.example" ]; then
        set -a
        . ./.env.example
        set +a
    fi
fi

echo "Development environment setup complete!" 