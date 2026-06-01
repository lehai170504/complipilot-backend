# CompliPilot Backend

CompliPilot is an **AI Compliance & Evidence OS** for managing compliance controls, evidence, tasks, audit history, organization workspaces, and AI-assisted evidence review.

This repository contains the Spring Boot backend API and the local Docker Compose setup for infrastructure services.

## Tech Stack

* Java 21
* Spring Boot 4
* Spring Security + JWT
* PostgreSQL 16
* Flyway migrations
* MinIO object storage
* FastAPI AI service integration
* Docker Compose

## Main Features

* Authentication with access and refresh tokens
* Organization workspaces and role-based access control
* Organization member management
* Compliance frameworks and requirements
* Compliance control lifecycle management
* Evidence metadata management
* Presigned upload and download URL flow for file evidence
* Evidence-to-control linking
* Compliance task tracking
* Audit trail for key compliance actions
* AI evidence analysis through the FastAPI AI service
* Persisted AI evidence review history
* AI missing-evidence recommendation for compliance controls

## Local Prerequisites

Install:

* Java 21
* Docker Desktop
* Node.js for the frontend
* Python 3.12 if running the AI service outside Docker

The Maven wrapper is already included in this repository.

## Recommended Repository Layout

Use this local folder structure:

```txt
D:\GitHub\
  complipilot-backend
  complipilot-frontend
  complipilot-ai-service
```

The backend Docker Compose file expects the AI service repository to be located at:

```txt
../complipilot-ai-service
```

## Environment Variables

Create a `.env` file in `complipilot-backend` based on `.env.example`.

Minimum local values:

```env
POSTGRES_DB=complipilot
POSTGRES_USER=complipilot
POSTGRES_PASSWORD=123456
POSTGRES_PORT=5433

MINIO_ROOT_USER=complipilot
MINIO_ROOT_PASSWORD=complipilot_minio_password
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001

AI_SERVICE_PORT=8000
AI_PROVIDER=rules
AI_FALLBACK_TO_RULES=true
GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.5-flash
```

To enable Gemini-backed AI review:

```env
AI_PROVIDER=gemini
GEMINI_API_KEY=your_real_gemini_api_key
AI_FALLBACK_TO_RULES=true
```

When `AI_FALLBACK_TO_RULES=true`, the AI service falls back to rule-based analysis if Gemini is unavailable.

## Run Local Infrastructure

From the backend repository:

```powershell
cd D:\GitHub\complipilot-backend

docker compose up -d --build --remove-orphans
docker compose ps
```

Expected services:

```txt
complipilot-backend-postgres
complipilot-backend-minio
complipilot-ai-service
```

Check AI service:

```powershell
curl.exe http://localhost:8000/health
```

Useful local service URLs:

```txt
AI Swagger:       http://localhost:8000/docs
AI Health:        http://localhost:8000/health
MinIO Console:   http://localhost:9001
PostgreSQL:      localhost:5433
```

## Run Backend

From the backend repository:

```powershell
cd D:\GitHub\complipilot-backend

.\mvnw.cmd spring-boot:run
```

Backend runs on:

```txt
http://localhost:8081
```

Useful backend URLs:

```txt
Backend Swagger: http://localhost:8081/swagger-ui/index.html
Actuator Health: http://localhost:8081/actuator/health
```

## Run Frontend

From the frontend repository:

```powershell
cd D:\GitHub\complipilot-frontend

npm install
npm run dev
```

Frontend runs on:

```txt
http://localhost:3000
```

## Local Development Flow

Start services in this order.

### 1. Start Docker infrastructure

```powershell
cd D:\GitHub\complipilot-backend

docker compose up -d --build --remove-orphans
```

This starts:

```txt
PostgreSQL
MinIO
FastAPI AI service
```

### 2. Start backend

```powershell
cd D:\GitHub\complipilot-backend

.\mvnw.cmd spring-boot:run
```

### 3. Start frontend

```powershell
cd D:\GitHub\complipilot-frontend

npm run dev
```

Then open:

```txt
http://localhost:3000
```

## AI Service Integration

The backend calls the AI service through:

```yml
app:
  ai:
    base-url: http://localhost:8000
```

For local development where the backend runs through Maven and the AI service runs through Docker, use:

```txt
http://localhost:8000
```

If the backend is also running inside Docker Compose in the future, use:

```txt
http://ai-service:8000
```

Frontend calls the backend only. The frontend does not call the AI service directly.

## Backend AI Endpoints

The backend exposes these AI-powered endpoints:

```http
POST /api/v1/organizations/{organizationId}/evidence/{evidenceId}/ai/analyze
GET  /api/v1/organizations/{organizationId}/evidence/{evidenceId}/ai/analysis/latest
GET  /api/v1/organizations/{organizationId}/evidence/{evidenceId}/ai/analyses

POST /api/v1/organizations/{organizationId}/compliance-items/{itemId}/ai/suggest-evidence
```

## Internal AI Service Endpoints

The AI service exposes these internal endpoints:

```http
POST /api/v1/ai/evidence/analyze
POST /api/v1/ai/compliance/suggest-evidence
```

## Evidence Workflow

Typical evidence workflow:

1. Create URL evidence or upload file evidence.
2. Link evidence to a compliance control.
3. Run AI evidence review.
4. Save and view the latest AI analysis.
5. Review AI analysis history.
6. Use compliance-item AI recommendations to identify missing evidence.
7. Update control notes, tasks, and status based on review outcome.

## Compliance Workflow

Typical compliance workflow:

1. Apply a framework or seed the security baseline.
2. Review generated compliance controls.
3. Update control status, notes, and due dates.
4. Link evidence to each control.
5. Run AI evidence coverage recommendation.
6. Create tasks for missing evidence or remediation work.
7. Review audit trail for key actions.

## MinIO

MinIO is used for local object storage.

Console:

```txt
http://localhost:9001
```

Default local credentials depend on `.env`:

```env
MINIO_ROOT_USER=complipilot
MINIO_ROOT_PASSWORD=complipilot_minio_password
```

## Database Migrations

Flyway migrations are stored in:

```txt
src/main/resources/db/migration
```

Run the backend and Flyway applies pending migrations automatically.

Current notable migrations include:

* Authentication and user tables
* Organization and membership tables
* Compliance framework and requirement tables
* Compliance item tables
* Evidence document and evidence link tables
* Compliance task tables
* Audit event tables
* AI evidence analysis history table

## Build and Test

Compile backend:

```powershell
cd D:\GitHub\complipilot-backend

.\mvnw.cmd -DskipTests package
```

Run tests:

```powershell
.\mvnw.cmd test
```

## Useful Local URLs

```txt
Frontend:        http://localhost:3000
Backend API:     http://localhost:8081
Backend Swagger: http://localhost:8081/swagger-ui/index.html
Actuator Health: http://localhost:8081/actuator/health
AI Swagger:      http://localhost:8000/docs
AI Health:       http://localhost:8000/health
MinIO Console:   http://localhost:9001
PostgreSQL:      localhost:5433
```

## Common Issues

### Container name already in use

If Docker reports:

```txt
container name "/complipilot-ai-service" is already in use
```

Run:

```powershell
docker rm -f complipilot-ai-service

cd D:\GitHub\complipilot-backend
docker compose up -d --build --remove-orphans
```

### Orphan containers

If Docker reports orphan containers, run:

```powershell
cd D:\GitHub\complipilot-backend

docker compose up -d --build --remove-orphans
```

### Port 8000 already in use

If AI service port is busy, stop the local uvicorn process or Docker container:

```powershell
docker stop complipilot-ai-service
```

### Port 8081 already in use

Find the process:

```powershell
netstat -ano | findstr :8081
```

Then stop the process or change backend port.

### AI service returns rule-based output

Check `.env`:

```env
AI_PROVIDER=gemini
GEMINI_API_KEY=your_real_key
AI_FALLBACK_TO_RULES=true
```

If the key is missing, invalid, or quota is unavailable, the service may fall back to rule-based review.

### PostgreSQL data not resetting

Docker volumes persist data. To reset local database and MinIO data:

```powershell
cd D:\GitHub\complipilot-backend

docker compose down -v
docker compose up -d --build
```

Use this carefully because it deletes local PostgreSQL and MinIO data.

## Recommended Commit Checks

Before pushing backend changes:

```powershell
cd D:\GitHub\complipilot-backend

.\mvnw.cmd -DskipTests package
```

Before pushing frontend changes:

```powershell
cd D:\GitHub\complipilot-frontend

npm run lint
npm run build
```

Before pushing AI service changes:

```powershell
cd D:\GitHub\complipilot-ai-service

.venv\Scripts\activate
python -m compileall app
```

## Related Repositories

Recommended local setup:

```txt
complipilot-backend      Spring Boot backend API and local Docker Compose
complipilot-frontend     Next.js frontend
complipilot-ai-service   FastAPI AI service
```
