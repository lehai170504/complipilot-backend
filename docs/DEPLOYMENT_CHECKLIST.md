# CompliPilot Deployment Checklist

This checklist describes how to prepare CompliPilot for deployment.

CompliPilot has two applications:

```txt
complipilot-backend
complipilot-frontend
```

Recommended deployment target:

```txt
Backend: Render
Frontend: Vercel
Database: Neon PostgreSQL or Render PostgreSQL
Object Storage: S3-compatible object storage
```

---

## Deployment Goals

The production deployment should provide:

```txt
Public frontend URL
Public backend API URL
Production PostgreSQL database
Production object storage
Working authentication
Working evidence upload/download
CORS configured correctly
Secure secrets
Stable environment variables
```

---

## Deployment Order

Recommended order:

```txt
1. Prepare backend production environment.
2. Provision PostgreSQL database.
3. Provision object storage.
4. Deploy backend.
5. Verify backend health.
6. Deploy frontend.
7. Configure backend CORS for frontend domain.
8. Test full production flow.
```

---

## Pre-Deployment Checklist

Before deploying:

```txt
Backend tests pass.
Frontend lint passes.
Frontend build passes.
No localhost hardcoded outside env files.
Production JWT secret is strong.
Production database URL is ready.
Production object storage credentials are ready.
CORS frontend domain is known.
Swagger disabled or restricted in production.
Demo defaults disabled in production frontend.
```

Backend verification:

```powershell
cd D:\GitHub\complipilot-backend
.\mvnw.cmd clean test
```

Frontend verification:

```powershell
cd D:\GitHub\complipilot-frontend
npm run lint
npm run build
```

---

## Backend Environment Variables

Required backend production env:

```env
PORT=8080
SPRING_PROFILES_ACTIVE=prod

DATABASE_URL=jdbc:postgresql://<host>:<port>/<database>?sslmode=require
DATABASE_USERNAME=<database-user>
DATABASE_PASSWORD=<database-password>

JWT_SECRET=<strong-production-secret>
JWT_ISSUER=complipilot-backend
JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=3600

APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.vercel.app

MINIO_ENDPOINT=<internal-or-provider-endpoint>
MINIO_PUBLIC_ENDPOINT=<public-object-storage-endpoint>
MINIO_ACCESS_KEY=<object-storage-access-key>
MINIO_SECRET_KEY=<object-storage-secret-key>
MINIO_BUCKET_EVIDENCE=complipilot-evidence
MINIO_PRESIGNED_URL_EXPIRATION_MINUTES=15

SPRINGDOC_ENABLED=false
```

Optional local-like variables if using Docker services:

```env
POSTGRES_DB=complipilot
POSTGRES_USER=complipilot
POSTGRES_PASSWORD=<password>
POSTGRES_PORT=5432

MINIO_ROOT_USER=<user>
MINIO_ROOT_PASSWORD=<password>
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
```

---

## Database Checklist

Recommended production database:

```txt
Neon PostgreSQL
Render PostgreSQL
Supabase PostgreSQL
Railway PostgreSQL
```

Checklist:

```txt
Database created
User created
Password saved securely
Connection string copied
SSL mode enabled if required
Backend can connect
Migrations/schema generation works
```

If using Neon, JDBC URL often needs:

```txt
sslmode=require
```

Example:

```env
DATABASE_URL=jdbc:postgresql://ep-example.ap-southeast-1.aws.neon.tech/complipilot?sslmode=require
```

---

## Object Storage Checklist

Evidence upload/download requires S3-compatible object storage.

Production options:

```txt
AWS S3
Cloudflare R2
DigitalOcean Spaces
Backblaze B2 S3-compatible
MinIO on VPS
```

Checklist:

```txt
Bucket created
Access key created
Secret key created
Backend endpoint configured
Public endpoint configured
CORS configured for frontend domain
Presigned PUT works
Presigned GET works
```

Bucket name:

```txt
complipilot-evidence
```

---

## Object Storage CORS

The browser uploads files directly to presigned URLs, so object storage CORS must allow the frontend origin.

Allowed origin:

```txt
https://your-frontend-domain.vercel.app
```

Allowed methods:

```txt
GET
PUT
POST
DELETE
HEAD
```

Allowed headers:

```txt
*
```

Expose headers:

```txt
ETag
```

Example CORS policy:

```json
[
  {
    "AllowedOrigins": ["https://your-frontend-domain.vercel.app"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"]
  }
]
```

For local development, allowed origin:

```txt
http://localhost:3000
```

---

## Backend Render Deployment

Recommended Render service type:

```txt
Web Service
```

Build command:

```bash
./mvnw clean package -DskipTests
```

Start command:

```bash
java -jar target/*.jar
```

Environment:

```txt
Java 21
```

Render env variables:

```env
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=<production-jdbc-url>
DATABASE_USERNAME=<db-user>
DATABASE_PASSWORD=<db-password>
JWT_SECRET=<strong-secret>
JWT_ISSUER=complipilot-backend
JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=3600
APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.vercel.app
MINIO_ENDPOINT=<storage-endpoint>
MINIO_PUBLIC_ENDPOINT=<storage-public-endpoint>
MINIO_ACCESS_KEY=<storage-access-key>
MINIO_SECRET_KEY=<storage-secret-key>
MINIO_BUCKET_EVIDENCE=complipilot-evidence
MINIO_PRESIGNED_URL_EXPIRATION_MINUTES=15
SPRINGDOC_ENABLED=false
```

After deployment, verify:

```txt
GET https://your-backend-domain.com/api/v1/health
GET https://your-backend-domain.com/actuator/health
GET https://your-backend-domain.com/actuator/info
```

---

## Backend Docker Deployment

If deploying with Docker:

```powershell
cd D:\GitHub\complipilot-backend
docker build -t complipilot-backend:prod .
```

Run with env:

```bash
docker run --env-file .env.prod -p 8080:8080 complipilot-backend:prod
```

For production compose:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Verify:

```bash
docker ps
docker logs <backend-container>
```

---

## Frontend Environment Variables

Required frontend production env:

```env
NEXT_PUBLIC_API_BASE_URL=https://your-backend-domain.com
NEXT_PUBLIC_APP_ENV=production
NEXT_PUBLIC_ENABLE_DEMO_DEFAULTS=false
```

Local frontend env:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8081
NEXT_PUBLIC_APP_ENV=local
NEXT_PUBLIC_ENABLE_DEMO_DEFAULTS=true
```

---

## Frontend Vercel Deployment

Vercel settings:

```txt
Framework: Next.js
Build command: npm run build
Install command: npm install
Output directory: default
```

Vercel env variables:

```env
NEXT_PUBLIC_API_BASE_URL=https://your-backend-domain.com
NEXT_PUBLIC_APP_ENV=production
NEXT_PUBLIC_ENABLE_DEMO_DEFAULTS=false
```

After deploy, note frontend domain:

```txt
https://your-frontend-domain.vercel.app
```

Then update backend env:

```env
APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.vercel.app
```

Redeploy backend after changing CORS.

---

## CORS Checklist

Backend must allow frontend origin.

Local:

```env
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Production:

```env
APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.vercel.app
```

Allowed headers:

```txt
Authorization
Content-Type
X-Request-Id
```

Exposed headers:

```txt
X-Request-Id
```

Common CORS issues:

```txt
Frontend URL has trailing slash mismatch
Backend env not updated
Backend not redeployed after CORS change
Object storage CORS missing for upload
```

---

## Auth Production Checklist

Current frontend stores tokens in client-readable cookies.

Current MVP cookie settings:

```txt
sameSite: strict
secure: true in production
expires: 30 days
path: /
```

Production hardening recommended:

```txt
Move tokens to HttpOnly Secure SameSite cookies.
Use backend-for-frontend or Next.js route handlers.
Avoid exposing refresh tokens to JavaScript.
```

Before production demo:

```txt
Verify login works.
Verify refresh works after access token expires.
Verify logout clears session.
Verify protected routes redirect to login.
Verify old expired token does not block login.
```

---

## Evidence Upload Checklist

Local evidence upload requires:

```txt
Backend running
MinIO running
Bucket exists
MINIO_ENDPOINT correct
MINIO_PUBLIC_ENDPOINT correct
Object storage CORS configured
```

Local env:

```env
MINIO_ENDPOINT=http://localhost:9000
MINIO_PUBLIC_ENDPOINT=http://localhost:9000
MINIO_BUCKET_EVIDENCE=complipilot-evidence
```

Production env:

```env
MINIO_ENDPOINT=<provider-endpoint>
MINIO_PUBLIC_ENDPOINT=<provider-public-endpoint>
MINIO_BUCKET_EVIDENCE=complipilot-evidence
```

Test flow:

```txt
Upload file evidence
Verify metadata created
Filter Source type = File
Download file evidence
Archive evidence
```

---

## Evidence Download Checklist

Download uses presigned GET URL.

Checklist:

```txt
Backend can generate download URL.
Object key exists.
Bucket policy allows presigned GET.
Browser opens download URL.
File content is correct.
```

If download fails:

```txt
Check object storage endpoint.
Check public endpoint.
Check bucket name.
Check object key.
Check backend logs.
```

---

## Production Smoke Test

After both backend and frontend are deployed:

```txt
1. Open frontend production URL.
2. Register new account.
3. Login.
4. Seed demo workspace.
5. Open dashboard.
6. Open compliance page.
7. Update compliance status.
8. Create URL evidence.
9. Upload file evidence.
10. Download file evidence.
11. Link evidence to compliance control.
12. Create task.
13. Update task.
14. Open audit page.
15. Confirm actions appear.
16. Logout.
17. Login again.
18. Refresh protected routes.
```

---

## Monitoring Checklist

Minimum monitoring for MVP:

```txt
Backend logs accessible
Frontend deployment logs accessible
Database connection errors visible
Object storage errors visible
Request ID shown in frontend error states
Request ID present in backend logs
```

Useful logs:

```bash
docker logs <backend-container> --tail 200
docker logs <minio-container> --tail 100
```

Render:

```txt
Use Logs tab in Render dashboard.
```

Vercel:

```txt
Use Deployment Logs and Function Logs.
```

---

## Security Checklist

Before public demo:

```txt
Use strong JWT_SECRET.
Disable Swagger in production.
Use HTTPS URLs.
Set frontend production env correctly.
Restrict CORS to frontend domain.
Do not commit .env.local.
Do not expose database password.
Do not expose object storage secret.
Use secure object storage credentials.
Verify refresh token rotation.
Verify logout revokes refresh token.
```

Recommended future security improvements:

```txt
HttpOnly cookie auth
CSRF protection for cookie auth
Organization invitation flow
Fine-grained permission UI
Rate limit tuning
Audit metadata viewer
```

---

## Rollback Checklist

If production deploy fails:

```txt
Check backend health endpoint.
Check backend logs.
Check database connection.
Check object storage credentials.
Check CORS env.
Check frontend NEXT_PUBLIC_API_BASE_URL.
Rollback frontend to previous Vercel deployment.
Rollback backend to previous Render deployment.
```

---

## Final Pre-Demo Checklist

Backend:

```txt
Health OK
Auth OK
Database OK
Object storage OK
CORS OK
Audit events OK
```

Frontend:

```txt
Build OK
Login OK
Dashboard OK
Compliance OK
Evidence OK
Tasks OK
Audit OK
Logout OK
```

Demo data:

```txt
Create a fresh account.
Seed demo workspace.
Prepare one URL evidence.
Prepare one small file evidence.
Create one task.
Update one compliance control.
Open audit page to show traceability.
```

---

## Recommended Next Work After Deployment

Priority:

```txt
K1 — HttpOnly cookie auth layer
K2 — Organization member invitations
K3 — Compliance report PDF export
K4 — Evidence detail drawer
K5 — Audit metadata viewer
K6 — Task reminders
K7 — AI evidence extraction
K8 — Compliance assistant
```
