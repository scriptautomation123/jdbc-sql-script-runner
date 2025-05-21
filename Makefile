# Variables
COMPOSE_FILE := .devcontainer/docker-compose.yml
COMPOSE_CMD := docker compose -f $(COMPOSE_FILE)

# Properties - Versions and Settings
export COMPOSE_VERSION = 3.8
export ORACLE_VERSION = 23-slim-faststart
export POSTGRES_VERSION = latest
export APP_VERSION = 1.0-SNAPSHOT

# Database Properties
export ORACLE_PORT = 1521
export ORACLE_PDB = FREEPDB1
export ORACLE_SYSTEM_PASSWORD = SecurePassword
export ORACLE_HR_PASSWORD = HR
export POSTGRES_PORT = 5432
export POSTGRES_PASSWORD = postgres
export POSTGRES_USER = postgres
export POSTGRES_DB = postgres

# Colors for output
CYAN := \033[36m
GREEN := \033[32m
RESET := \033[0m

# Basic Docker Commands
.PHONY: clean
clean:
	@echo "$(CYAN)Stopping and removing containers, networks, and volumes...$(RESET)"
	$(COMPOSE_CMD) down -v
	@echo "$(GREEN)Clean completed$(RESET)"

.PHONY: prune
prune:
	@echo "$(CYAN)Pruning Docker system...$(RESET)"
	docker system prune -f
	docker builder prune --all -f
	@echo "$(GREEN)Prune completed$(RESET)"

.PHONY: build
build:
	@echo "$(CYAN)Building services without cache...$(RESET)"
	$(COMPOSE_CMD) build --no-cache
	@echo "$(GREEN)Build completed$(RESET)"

.PHONY: up
up:
	@echo "$(CYAN)Starting services...$(RESET)"
	$(COMPOSE_CMD) up -d
	@echo "$(GREEN)Services started$(RESET)"

.PHONY: status
status:
	@echo "$(CYAN)Checking service status...$(RESET)"
	$(COMPOSE_CMD) ps
	@echo "$(GREEN)Status check completed$(RESET)"

.PHONY: logs
logs:
	@echo "$(CYAN)Showing logs...$(RESET)"
	$(COMPOSE_CMD) logs -f

# Database Readiness Check
.PHONY: analyze-oracle-logs
analyze-oracle-logs:
	@echo "$(CYAN)Analyzing Oracle startup patterns...$(RESET)"
	@docker logs oracledb 2>&1 | grep -A 5 "Starting Oracle Database" || true
	@docker logs oracledb 2>&1 | grep "DATABASE IS READY TO USE" || true
	@docker logs oracledb 2>&1 | grep "FREEPDB1 IS READY TO USE" || true
	@docker logs oracledb 2>&1 | grep "Oracle Database.*is ready" || true

.PHONY: analyze-postgres-logs
analyze-postgres-logs:
	@echo "$(CYAN)Analyzing PostgreSQL startup patterns...$(RESET)"
	@docker logs postgresdb 2>&1 | grep "database system was shut down" || true
	@docker logs postgresdb 2>&1 | grep "database system is ready" || true
	@docker logs postgresdb 2>&1 | grep "listening on IPv4" || true

.PHONY: wait-for-db
wait-for-db:
	@echo "$(CYAN)Checking Oracle database initialization...$(RESET)"
	@echo "$(CYAN)Oracle startup sequence:$(RESET)"
	@make analyze-oracle-logs || echo "Database not started yet..."
	@while ! docker logs oracledb 2>&1 | grep -q "DATABASE IS READY TO USE!"; do \
		echo "Waiting for Oracle startup completion..."; \
		make analyze-oracle-logs 2>/dev/null || true; \
		if ! docker ps | grep -q oracledb; then \
			echo "Oracle container not running yet..."; \
		fi; \
		sleep 5; \
	done
	@echo "$(GREEN)Oracle database initialization completed:$(RESET)"
	@make analyze-oracle-logs
	
	@echo "$(CYAN)Checking PostgreSQL database initialization...$(RESET)"
	@echo "$(CYAN)PostgreSQL startup sequence:$(RESET)"
	@make analyze-postgres-logs || echo "Database not started yet..."
	@while ! docker logs postgresdb 2>&1 | grep -q "database system is ready to accept connections"; do \
		echo "Waiting for PostgreSQL startup completion..."; \
		make analyze-postgres-logs 2>/dev/null || true; \
		if ! docker ps | grep -q postgresdb; then \
			echo "PostgreSQL container not running yet..."; \
		fi; \
		sleep 2; \
	done
	@echo "$(GREEN)PostgreSQL database initialization completed:$(RESET)"
	@make analyze-postgres-logs
	
	@echo "$(GREEN)Database initialization patterns documented for future reference$(RESET)"

# Document database initialization patterns
.PHONY: document-db-patterns
document-db-patterns:
	@mkdir -p .cursor/memory-bank
	@echo "# Database Initialization Patterns" > .cursor/memory-bank/db_patterns.md
	@echo "\n## Oracle Database" >> .cursor/memory-bank/db_patterns.md
	@echo "\`\`\`" >> .cursor/memory-bank/db_patterns.md
	@make analyze-oracle-logs >> .cursor/memory-bank/db_patterns.md 2>&1 || true
	@echo "\`\`\`" >> .cursor/memory-bank/db_patterns.md
	@echo "\n## PostgreSQL Database" >> .cursor/memory-bank/db_patterns.md
	@echo "\`\`\`" >> .cursor/memory-bank/db_patterns.md
	@make analyze-postgres-logs >> .cursor/memory-bank/db_patterns.md 2>&1 || true
	@echo "\`\`\`" >> .cursor/memory-bank/db_patterns.md

# Oracle Testing Commands
.PHONY: test-oracle-system
test-oracle-system:
	@echo "$(CYAN)Testing Oracle SYSTEM schema access...$(RESET)"	
	@echo "SELECT name, open_mode FROM v\$$database;\nSELECT username FROM dba_users WHERE account_status = 'OPEN';\nEXIT;" | docker exec -i oracledb sqlplus -S system/$(ORACLE_SYSTEM_PASSWORD)@localhost:$(ORACLE_PORT)/$(ORACLE_PDB)

.PHONY: test-oracle-hr
test-oracle-hr:
	@echo "$(CYAN)Testing Oracle HR schema access...$(RESET)"
	@echo "SELECT table_name FROM user_tables;\nSELECT COUNT(*) FROM employees;\nEXIT;" | docker exec -i oracledb sqlplus -S HR/$(ORACLE_HR_PASSWORD)@localhost:$(ORACLE_PORT)/$(ORACLE_PDB)

.PHONY: test-oracle-connections-all
test-oracle-connections-all: test-oracle-system test-oracle-hr
	@echo "$(GREEN)All Oracle connection tests completed$(RESET)"

# PostgreSQL Testing Commands
.PHONY: test-postgres-admin
test-postgres-admin:
	@echo "$(CYAN)Testing PostgreSQL admin access...$(RESET)"
	@docker exec postgresdb psql -U $(POSTGRES_USER) -d $(POSTGRES_DB) -c "SELECT version();"
	@docker exec postgresdb psql -U $(POSTGRES_USER) -d $(POSTGRES_DB) -c "SELECT usename, usesuper FROM pg_user;"
	@docker exec postgresdb psql -U $(POSTGRES_USER) -d $(POSTGRES_DB) -c "SELECT nspname FROM pg_namespace WHERE nspname = 'hr';"

.PHONY: test-postgres-hr
test-postgres-hr:
	@echo "$(CYAN)Testing PostgreSQL HR schema access...$(RESET)"
	@docker exec postgresdb psql -U hr -d $(POSTGRES_DB) -c "\dt hr.*"
	@docker exec postgresdb psql -U hr -d $(POSTGRES_DB) -c "SET search_path TO hr; SELECT COUNT(*) FROM employees;"
	@docker exec postgresdb psql -U hr -d $(POSTGRES_DB) -c "SET search_path TO hr; SELECT table_name FROM information_schema.tables WHERE table_schema = 'hr';"

.PHONY: test-postgres-connections-all
test-postgres-connections-all: test-postgres-admin test-postgres-hr
	@echo "$(GREEN)All PostgreSQL connection tests completed$(RESET)"

# Combined Database Tests
.PHONY: test-all-db
test-all-db: test-oracle-connections-all test-postgres-connections-all
	@echo "$(GREEN)All database connection tests completed successfully$(RESET)"

# Combined tasks
.PHONY: all
all: clean prune build up status
	@echo "$(GREEN)All tasks completed successfully!$(RESET)"
	@echo "$(CYAN)Use 'make logs' to view service logs$(RESET)"

.PHONY: make-all
make-all: all wait-for-db document-db-patterns test-all-db
	@echo "$(GREEN)All services are up, tested, and documented successfully!$(RESET)"

# Database Connection Details
.PHONY: oracle-connection-details
oracle-connection-details:
	@echo "$(CYAN)Oracle thin client connection details:$(RESET)"
	@echo "$(GREEN)JDBC Connection String:$(RESET)"
	@echo "  jdbc:oracle:thin:@<host>:$(ORACLE_PORT)/$(ORACLE_PDB)"
	@echo "$(GREEN)For local connections:$(RESET)"
	@echo "  jdbc:oracle:thin:@localhost:$(ORACLE_PORT)/$(ORACLE_PDB)"
	@echo "  jdbc:oracle:thin:HR/$(ORACLE_HR_PASSWORD)@localhost:$(ORACLE_PORT)/$(ORACLE_PDB)"
	@echo "$(GREEN)For Docker internal connections:$(RESET)"
	@echo "  jdbc:oracle:thin:@oracledb:$(ORACLE_PORT)/$(ORACLE_PDB)"
	@echo "  jdbc:oracle:thin:HR/$(ORACLE_HR_PASSWORD)@oracledb:$(ORACLE_PORT)/$(ORACLE_PDB)"
	@echo "$(GREEN)Connection parameters:$(RESET)"
	@echo "  Service Name: $(ORACLE_PDB)"
	@echo "  Port: $(ORACLE_PORT)"
	@echo "  System User: system"
	@echo "  System Password: $(ORACLE_SYSTEM_PASSWORD)"
	@echo "  App User: HR"
	@echo "  App Password: $(ORACLE_HR_PASSWORD)"
	@echo "$(GREEN)TNS Connection String Format:$(RESET)"
	@echo "  <host>:$(ORACLE_PORT)/$(ORACLE_PDB)"
	@echo "$(GREEN)SQLPlus Connection Commands:$(RESET)"
	@echo "  sqlplus system/$(ORACLE_SYSTEM_PASSWORD)@<host>:$(ORACLE_PORT)/$(ORACLE_PDB)"
	@echo "  sqlplus system/$(ORACLE_SYSTEM_PASSWORD)@localhost:$(ORACLE_PORT)/$(ORACLE_PDB)"
	@echo "  sqlplus HR/$(ORACLE_HR_PASSWORD)@<host>:$(ORACLE_PORT)/$(ORACLE_PDB)"
	@echo "  sqlplus HR/$(ORACLE_HR_PASSWORD)@localhost:$(ORACLE_PORT)/$(ORACLE_PDB)"
	@echo "$(GREEN)TNS Entry Example:$(RESET)"
	@echo "  ORCLPDB = "
	@echo "    (DESCRIPTION = "
	@echo "      (ADDRESS = (PROTOCOL = TCP)(HOST = <host>)(PORT = $(ORACLE_PORT)))"
	@echo "      (CONNECT_DATA = "
	@echo "        (SERVER = DEDICATED)"
	@echo "        (SERVICE_NAME = $(ORACLE_PDB))"
	@echo "      )"
	@echo "    )"

.PHONY: postgres-connection-details
postgres-connection-details:
	@echo "$(CYAN)PostgreSQL connection details:$(RESET)"
	@echo "$(GREEN)JDBC Connection String:$(RESET)"
	@echo "  jdbc:postgresql://<host>:$(POSTGRES_PORT)/$(POSTGRES_DB)"
	@echo "$(GREEN)For local connections:$(RESET)"
	@echo "  jdbc:postgresql://localhost:$(POSTGRES_PORT)/$(POSTGRES_DB)"
	@echo "$(GREEN)For Docker internal connections:$(RESET)"
	@echo "  jdbc:postgresql://postgresdb:$(POSTGRES_PORT)/$(POSTGRES_DB)"
	@echo "$(GREEN)Connection parameters:$(RESET)"
	@echo "  Database: $(POSTGRES_DB)"
	@echo "  Port: $(POSTGRES_PORT)"
	@echo "  Admin User: $(POSTGRES_USER)"
	@echo "  Admin Password: $(POSTGRES_PASSWORD)"
	@echo "  Schema: hr (for HR application data)"
	@echo "$(GREEN)Connection URL Format:$(RESET)"
	@echo "  postgresql://<user>:<password>@<host>:$(POSTGRES_PORT)/$(POSTGRES_DB)"
	@echo "$(GREEN)psql Connection Commands:$(RESET)"
	@echo "  psql -U $(POSTGRES_USER) -h <host> -p $(POSTGRES_PORT) -d $(POSTGRES_DB)"
	@echo "  psql -U hr -h <host> -p $(POSTGRES_PORT) -d $(POSTGRES_DB)"
	@echo "$(GREEN)Connection String Examples:$(RESET)"
	@echo "  Admin: postgresql://$(POSTGRES_USER):$(POSTGRES_PASSWORD)@<host>:$(POSTGRES_PORT)/$(POSTGRES_DB)"
	@echo "  Schema-specific: postgresql://hr:password@<host>:$(POSTGRES_PORT)/$(POSTGRES_DB)?currentSchema=hr"

.PHONY: db-connection-details
db-connection-details: oracle-connection-details postgres-connection-details
	@echo "$(GREEN)All database connection details displayed$(RESET)"
	@echo "$(CYAN)Replace <host> with your actual hostname or IP address$(RESET)"

.PHONY: save-connection-details
save-connection-details:
	@mkdir -p .cursor/connection-details
	@make oracle-connection-details > .cursor/connection-details/oracle-connection.txt
	@make postgres-connection-details > .cursor/connection-details/postgres-connection.txt
	@echo "$(GREEN)Connection details saved to .cursor/connection-details/$(RESET)"

# UnifiedDatabaseRunner Command Generation
.PHONY: db-client-command-oracle
db-client-command-oracle:
	@echo "$(CYAN)Oracle connection command for UnifiedDatabaseRunner:$(RESET)"
	@echo "$(GREEN)System User Command:$(RESET)"
	@echo "java -cp target/app.jar com.example.shelldemo.UnifiedDatabaseRunner \\"
	@echo "  --type oracle \\"
	@echo "  --connection-type thin \\"
	@echo "  --host localhost \\"
	@echo "  --port $(ORACLE_PORT) \\"
	@echo "  --username system \\"
	@echo "  --password $(ORACLE_SYSTEM_PASSWORD) \\"
	@echo "  --database $(ORACLE_PDB) \\"
	@echo "  --stop-on-error true \\"
	@echo "  --print-statements true \\"
	@echo "  <script_file_or_procedure_name>"
	@echo ""
	@echo "$(GREEN)HR User Command:$(RESET)"
	@echo "java -cp target/app.jar com.example.shelldemo.UnifiedDatabaseRunner \\"
	@echo "  --type oracle \\"
	@echo "  --connection-type thin \\"
	@echo "  --host localhost \\"
	@echo "  --port $(ORACLE_PORT) \\"
	@echo "  --username HR \\"
	@echo "  --password $(ORACLE_HR_PASSWORD) \\"
	@echo "  --database $(ORACLE_PDB) \\"
	@echo "  --stop-on-error true \\"
	@echo "  --print-statements true \\"
	@echo "  <script_file_or_procedure_name>"

.PHONY: db-client-command-postgres
db-client-command-postgres:
	@echo "$(CYAN)PostgreSQL connection command for UnifiedDatabaseRunner:$(RESET)"
	@echo "$(GREEN)Admin User Command:$(RESET)"
	@echo "java -cp target/app.jar com.example.shelldemo.UnifiedDatabaseRunner \\"
	@echo "  --type postgresql \\"
	@echo "  --host localhost \\"
	@echo "  --port $(POSTGRES_PORT) \\"
	@echo "  --username $(POSTGRES_USER) \\"
	@echo "  --password $(POSTGRES_PASSWORD) \\"
	@echo "  --database $(POSTGRES_DB) \\"
	@echo "  --stop-on-error true \\"
	@echo "  --print-statements true \\"
	@echo "  <script_file_or_procedure_name>"
	@echo ""
	@echo "$(GREEN)HR User Command:$(RESET)"
	@echo "java -cp target/app.jar com.example.shelldemo.UnifiedDatabaseRunner \\"
	@echo "  --type postgresql \\"
	@echo "  --host localhost \\"
	@echo "  --port $(POSTGRES_PORT) \\"
	@echo "  --username hr \\"
	@echo "  --password hr_password \\"
	@echo "  --database $(POSTGRES_DB) \\"
	@echo "  --stop-on-error true \\"
	@echo "  --print-statements true \\"
	@echo "  <script_file_or_procedure_name>"

.PHONY: db-client-stored-procedure-examples
db-client-stored-procedure-examples:
	@echo "$(CYAN)Stored Procedure/Function examples for UnifiedDatabaseRunner:$(RESET)"
	@echo "$(GREEN)Execute Oracle stored procedure:$(RESET)"
	@echo "java -cp target/app.jar com.example.shelldemo.UnifiedDatabaseRunner \\"
	@echo "  --type oracle \\"
	@echo "  --connection-type thin \\"
	@echo "  --host localhost \\"
	@echo "  --port $(ORACLE_PORT) \\"
	@echo "  --username HR \\"
	@echo "  --password $(ORACLE_HR_PASSWORD) \\"
	@echo "  --database $(ORACLE_PDB) \\"
	@echo "  --input \"p_employee_id:NUMBER:101,p_department_id:NUMBER:10\" \\"
	@echo "  --output \"p_salary:NUMBER,p_hire_date:DATE\" \\"
	@echo "  update_employee_details"
	@echo ""
	@echo "$(GREEN)Execute PostgreSQL function with return value:$(RESET)"
	@echo "java -cp target/app.jar com.example.shelldemo.UnifiedDatabaseRunner \\"
	@echo "  --type postgresql \\"
	@echo "  --host localhost \\"
	@echo "  --port $(POSTGRES_PORT) \\"
	@echo "  --username $(POSTGRES_USER) \\"
	@echo "  --password $(POSTGRES_PASSWORD) \\"
	@echo "  --database $(POSTGRES_DB) \\"
	@echo "  --function \\"
	@echo "  --return-type INTEGER \\"
	@echo "  --input \"employee_id:INTEGER:101,department_id:INTEGER:10\" \\"
	@echo "  hr.get_employee_salary"

.PHONY: db-client-commands
db-client-commands: db-client-command-oracle db-client-command-postgres db-client-stored-procedure-examples
	@echo "$(GREEN)All UnifiedDatabaseRunner command examples displayed$(RESET)"
	@echo "$(CYAN)Replace <script_file_or_procedure_name> with actual file or procedure$(RESET)"

.PHONY: save-client-commands
save-client-commands:
	@mkdir -p .cursor/client-commands
	@make db-client-command-oracle > .cursor/client-commands/oracle-client-commands.txt
	@make db-client-command-postgres > .cursor/client-commands/postgres-client-commands.txt
	@make db-client-stored-procedure-examples > .cursor/client-commands/stored-procedure-examples.txt
	@echo "$(GREEN)Client commands saved to .cursor/client-commands/$(RESET)"

# Help command
.PHONY: help
help:
	@echo "$(CYAN)Available commands:$(RESET)"
	@echo "  make clean                - Stop and remove all containers, networks, and volumes"
	@echo "  make prune               - Clean up unused Docker resources"
	@echo "  make build               - Build all services without using cache"
	@echo "  make up                  - Start all services"
	@echo "  make status              - Check status of all services"
	@echo "  make logs                - View logs from all services"
	@echo "  make all                 - Run all tasks in sequence (clean, prune, build, up, status)"
	@echo "  make wait-for-db         - Wait for databases and analyze startup patterns"
	@echo "  make analyze-oracle-logs - Analyze Oracle database startup patterns"
	@echo "  make analyze-postgres-logs - Analyze PostgreSQL startup patterns"
	@echo "  make document-db-patterns - Document database initialization patterns"
	@echo "  make test-oracle-system  - Test Oracle SYSTEM schema access"
	@echo "  make test-oracle-hr      - Test Oracle HR schema access"
	@echo "  make test-oracle-connections-all - Test all Oracle connections"
	@echo "  make test-postgres-admin - Test PostgreSQL admin access"
	@echo "  make test-postgres-hr    - Test PostgreSQL HR schema access"
	@echo "  make test-postgres-connections-all - Test all PostgreSQL connections"
	@echo "  make test-all-db         - Test all database connections"
	@echo "  make make-all            - Run all tasks, document patterns, and test connections"
	@echo "  make oracle-connection-details - Display Oracle thin client connection details"
	@echo "  make postgres-connection-details - Display PostgreSQL connection details"
	@echo "  make db-connection-details - Display all database connection details"
	@echo "  make save-connection-details - Save connection details to files"
	@echo "  make db-client-command-oracle - Show Oracle command for UnifiedDatabaseRunner"
	@echo "  make db-client-command-postgres - Show PostgreSQL command for UnifiedDatabaseRunner"
	@echo "  make db-client-stored-procedure-examples - Show stored procedure examples"
	@echo "  make db-client-commands - Show all UnifiedDatabaseRunner command examples"
	@echo "  make save-client-commands - Save client commands to files"
	@echo "  make help                - Show this help message"