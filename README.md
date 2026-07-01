# CompliPilot — AI Compliance & Evidence OS

CompliPilot is an AI-assisted compliance and evidence management platform for organizations.

It helps teams manage:

- Organizations and workspaces
- Compliance requirements
- Evidence documents
- Evidence file upload/download
- Audit trails
- AI-assisted evidence analysis

The project is built as a production-style full-stack system using:

- Frontend: Next.js, TypeScript, Tailwind CSS, shadcn/ui
- Backend: Spring Boot, PostgreSQL, JWT authentication
- AI Service: FastAPI
- Database: Neon PostgreSQL
- File Storage: Supabase Storage
- Deployment: Vercel + Render + Neon + Supabase

---

## 1. Production Architecture

```txt
User Browser
    ↓
Vercel Frontend
    ↓
Render Backend API
    ↓
Neon PostgreSQL

Render Backend API
    ↓
Supabase Storage

Render Backend API
    ↓
Render AI Service
```

### Services

| Service | Platform | Purpose |
|---|---|---|
| Frontend | Vercel | User interface |
| Backend API | Render | Auth, organizations, compliance, evidence, audit |
| AI Service | Render | Evidence analysis |
| Database | Neon | PostgreSQL production database |
| Storage | Supabase Storage | Private evidence file storage |

---

## 2. Core Features

### Authentication & Identity

- Register, Login, Logout
- JWT access token & refresh token rotation
- User profile management
- Change password & profile activity tracking

### Organizations & Workspaces

- Organization/workspace context
- Member-based access control (Owner, Manager, Member)
- Organization invitations and onboarding
- Tenant-level data isolation

### Billing & Subscription

- SaaS subscription plans (Free, Pro, Enterprise)
- Stripe checkout integration
- Usage quotas and limits tracking (members, storage, AI credits)

### Compliance & Tasks Management

- Compliance frameworks and localized requirements
- Apply frameworks to organizations
- Track company compliance items status
- Assign compliance tasks and prioritize actions

### Evidence Management

- Create evidence metadata
- Secure upload/download through Supabase/MinIO signed URLs
- Versioning and archiving evidence
- Cross-link evidence to compliance items

### AI Evidence Analysis

- Analyze evidence using an external AI microservice
- Generate summary, risk level, and confidence score
- Discover findings, missing information, and suggested actions
- Maintain AI analysis history per evidence

### Notifications & Mail

- In-app notification center for users
- Email integration (Resend/SMTP) for invitations and alerts

### Reporting & Audit Trail

- System-wide audit logging for all critical operations
- CSV exports for compliance data
- Admin platform system status monitoring

---

## 3. Local Development Requirements

Install:

- Java 21
- Maven Wrapper
- Docker Desktop
- Node.js
- Python 3.12
- PostgreSQL through Docker Compose
- MinIO through Docker Compose

---

## 4. Backend Local Setup

```powershell
cd D:\GitHub\complipilot-backend

copy .env.example .env

docker compose up -d --build --remove-orphans

.\mvnw.cmd -DskipTests package
.\mvnw.cmd spring-boot:run
```

Backend local URL:

```txt
http://localhost:8081
```

Swagger local:

```txt
http://localhost:8081/swagger-ui/index.html
```

Health check:

```txt
http://localhost:8081/actuator/health
```

---

## 5. Frontend Local Setup

```powershell
cd D:\GitHub\complipilot-frontend

npm install
npm run dev
```

Frontend local URL:

```txt
http://localhost:3000
```

Required local frontend environment:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8081
NEXT_PUBLIC_SUPABASE_URL=https://your-project-ref.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=your_supabase_anon_key
NEXT_PUBLIC_SUPABASE_STORAGE_BUCKET=complipilot-evidence-prod
```

Important:

```txt
The frontend must only use the Supabase anon key.
Never expose the Supabase service role key in frontend environment variables.
```

---

## 6. AI Service Local Setup

```powershell
cd D:\GitHub\complipilot-ai-service

python -m venv .venv
.\.venv\Scripts\activate

pip install -r requirements.txt

uvicorn app.main:app --reload --port 8000
```

AI service local URL:

```txt
http://localhost:8000
```

Health check:

```txt
http://localhost:8000/health
```

Docs:

```txt
http://localhost:8000/docs
```

---

## 7. Backend Environment Variables

### Local Backend `.env`

```env
SPRING_PROFILES_ACTIVE=local

POSTGRES_DB=complipilot
POSTGRES_USER=complipilot
POSTGRES_PASSWORD=123456
POSTGRES_PORT=5433

API_PORT=8081

DATABASE_URL=jdbc:postgresql://localhost:5433/complipilot
DATABASE_USERNAME=complipilot
DATABASE_PASSWORD=123456

JWT_SECRET=change_this_to_a_long_random_secret_for_local_development
JWT_ISSUER=complipilot-backend
JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=3600
JWT_REFRESH_TOKEN_EXPIRATION_SECONDS=2592000
JWT_REFRESH_TOKEN_CLEANUP_FIXED_RATE_MS=3600000
JWT_REVOKED_REFRESH_TOKEN_RETENTION_SECONDS=604800

APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001

STORAGE_PROVIDER=minio

MINIO_ENDPOINT=http://localhost:9000
MINIO_PUBLIC_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=complipilot
MINIO_SECRET_KEY=complipilot_minio_password
MINIO_BUCKET_EVIDENCE=complipilot-evidence
MINIO_PRESIGNED_URL_EXPIRATION_MINUTES=15

MINIO_ROOT_USER=complipilot
MINIO_ROOT_PASSWORD=complipilot_minio_password
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001

AI_SERVICE_BASE_URL=http://localhost:8000

SPRINGDOC_ENABLED=true

RATE_LIMIT_ENABLED=true
RATE_LIMIT_AUTH_CAPACITY=20
RATE_LIMIT_AUTH_WINDOW_SECONDS=60

APP_DEMO_USERS_ENABLED=true
APP_VERSION=0.0.1-SNAPSHOT
```

### Production Backend on Render

```env
SPRING_PROFILES_ACTIVE=prod

DATABASE_URL=jdbc:postgresql://your-neon-host/neondb?sslmode=require
DATABASE_USERNAME=your_neon_user
DATABASE_PASSWORD=your_neon_password

JWT_SECRET=your_long_random_secret_at_least_64_chars
JWT_ISSUER=complipilot-backend
JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=3600
JWT_REFRESH_TOKEN_EXPIRATION_SECONDS=2592000
JWT_REFRESH_TOKEN_CLEANUP_FIXED_RATE_MS=3600000
JWT_REVOKED_REFRESH_TOKEN_RETENTION_SECONDS=604800

APP_CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app

STORAGE_PROVIDER=supabase

SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_SERVICE_ROLE_KEY=your_supabase_service_role_key
SUPABASE_STORAGE_BUCKET=complipilot-evidence-prod
SUPABASE_SIGNED_URL_EXPIRATION_SECONDS=900

AI_SERVICE_BASE_URL=https://your-ai-service.onrender.com

SPRINGDOC_ENABLED=false

RATE_LIMIT_ENABLED=true
RATE_LIMIT_AUTH_CAPACITY=10
RATE_LIMIT_AUTH_WINDOW_SECONDS=60

APP_DEMO_USERS_ENABLED=false
APP_VERSION=0.0.1-SNAPSHOT

JAVA_TOOL_OPTIONS=-Xms128m -Xmx384m -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom
```

Important:

- Do not manually set `PORT` on Render.
- Render injects `PORT` automatically.
- Do not commit `.env` or production secrets.
- Supabase `service_role` key must only be used by the backend.
- The frontend must only use Supabase anon key.

---

## 8. Frontend Environment Variables

### Production on Vercel

```env
NEXT_PUBLIC_API_BASE_URL=https://your-backend.onrender.com
NEXT_PUBLIC_SUPABASE_URL=https://your-project-ref.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=your_supabase_anon_key
NEXT_PUBLIC_SUPABASE_STORAGE_BUCKET=complipilot-evidence-prod
```

Important:

- `NEXT_PUBLIC_SUPABASE_ANON_KEY` is safe for browser usage.
- Never expose `SUPABASE_SERVICE_ROLE_KEY` in Vercel frontend variables.
- Redeploy Vercel after changing environment variables.

---

## 9. Evidence File Flow

### Upload Flow

```txt
Frontend
→ POST /api/v1/organizations/{organizationId}/evidence/upload-url
→ Backend creates Supabase signed upload URL
→ Frontend uploads file to Supabase Storage using signed upload token
→ Frontend creates evidence metadata in backend only after upload succeeds
```

Important:

```txt
Evidence metadata must not be created if file upload fails.
```

### Download Flow

```txt
Frontend
→ POST /api/v1/organizations/{organizationId}/evidence/{evidenceId}/download-url
→ Backend creates Supabase signed download URL
→ Frontend opens temporary signed URL
```

The storage bucket is private. Files are not served through public URLs.

---

## 10. AI Evidence Analysis Flow

```txt
Frontend
→ POST /api/v1/organizations/{organizationId}/evidence/{evidenceId}/ai/analyze
→ Backend validates organization access
→ Backend sends evidence context to AI service
→ AI service returns summary, findings, risk level, missing information, and suggested actions
→ Backend stores or returns the analysis
```

---

## 11. Deployment Checklist

### Backend Render

- Set production environment variables
- Connect GitHub repo
- Deploy latest commit
- Check health endpoint:

```txt
https://your-backend.onrender.com/actuator/health
```

### AI Service Render

- Set AI environment variables
- Deploy latest commit
- Check health endpoint:

```txt
https://your-ai-service.onrender.com/health
```

### Frontend Vercel

- Set frontend environment variables
- Redeploy latest commit
- Test login, upload, download, and AI analysis

### Supabase

- Create private bucket:

```txt
complipilot-evidence-prod
```

- Use service role key only in backend
- Use anon key only in frontend

### Neon

- Create PostgreSQL database
- Use JDBC connection string in backend Render env

---

## 12. Common Issues

### CORS from Frontend to Backend

Check:

```env
APP_CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app
```

No trailing slash.

Correct:

```txt
https://your-frontend.vercel.app
```

Wrong:

```txt
https://your-frontend.vercel.app/
```

### File Not Found in Storage

This usually means evidence metadata exists in the database, but the actual file object was not uploaded to Supabase Storage.

Fix:

```txt
Archive the broken evidence record
Upload the file again
```

### Invalid Supabase Signed URL

If the browser shows:

```txt
requested path is invalid
```

check that signed URLs include:

```txt
/storage/v1/object/...
```

### AI Service 502

Check:

```txt
AI_SERVICE_BASE_URL
AI service health endpoint
AI service Render logs
```

For stable demo mode, use rules-based AI fallback.

---

## 13. Useful Commands

Backend:

```powershell
cd D:\GitHub\complipilot-backend

.\mvnw.cmd -DskipTests package
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd D:\GitHub\complipilot-frontend

npm run lint
npm run build
npm run dev
```

AI service:

```powershell
cd D:\GitHub\complipilot-ai-service

.\.venv\Scripts\activate
uvicorn app.main:app --reload --port 8000
```

Git:

```powershell
git status
git add .
git commit -m "message"
git push
```

---

## 14. Production Security Notes

- Do not commit `.env`
- Do not expose JWT secret
- Do not expose Supabase service role key in frontend
- Keep storage bucket private
- Use signed URLs for upload/download
- Disable Swagger in production unless needed for demo
- Restrict CORS to frontend domain only
- Use strong JWT secret in production
- Rotate secrets if they were accidentally shared

---

## 15. Recommended Demo Flow

```txt
Login
→ Open workspace
→ Open Evidence
→ Upload new evidence file
→ Download evidence file
→ Link evidence to compliance item
→ Run AI analysis
→ View findings, missing information, and suggested actions
→ Close AI panel
→ View AI analysis history
```

Avoid using old evidence records created before the Supabase upload flow was fixed.
