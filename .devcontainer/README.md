# Java 21 + Oracle XE Development Container

A ready-to-use development container configuration for Java 21 and Oracle XE projects.

## Features

- Java 21 development environment
- Oracle XE 21 with HR schema
- Maven 3.8
- VS Code Java extensions
- Automatic database initialization

## Usage

1. Copy the `.devcontainer` folder to your Java project root
2. Open the project in VS Code
3. When prompted, click "Reopen in Container"

## Configuration

### Database Credentials

- System Password: `Oracle123`
- HR User: `HR`
- HR Password: `HR`
- Service Name: `XEPDB1`
- Port: `1521`

### Java/Maven Settings

- Java Home: `/usr/java/openjdk-21`
- Maven Path: `/usr/share/maven/bin/mvn`

## Customization

Modify these files as needed:

- `devcontainer.json`: VS Code settings and extensions
- `docker-compose.yml`: Container configuration
- `init-scripts/`: Database initialization scripts
