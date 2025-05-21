FROM gvenzl/oracle-free:23-slim-faststart

# Optional: Copy initialization scripts
COPY init-scripts/*.sql /container-entrypoint-initdb.d/

# Health check for docker-compose
HEALTHCHECK --interval=10s --timeout=5s --retries=10 CMD healthcheck.sh