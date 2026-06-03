# CompliPilot Demo Guide

This guide describes the recommended demo flow for presenting CompliPilot.

---

## 1. Demo Goal

The goal of the demo is to show how CompliPilot helps an organization manage compliance evidence and use AI to review whether uploaded evidence is useful, complete, and risky.

CompliPilot demonstrates a realistic compliance workflow:

```txt
Requirement
→ Evidence
→ Secure file storage
→ AI evidence review
→ Audit/history
```

---

## 2. Demo Environment

Production services:

```txt
Frontend: Vercel
Backend: Render
AI Service: Render
Database: Neon PostgreSQL
Storage: Supabase Storage
```

Before demo, check:

```txt
Frontend opens successfully
Backend /actuator/health is UP
AI service /health is UP
Supabase bucket exists
Demo account can login
```

---

## 3. Demo Flow

### Step 1 — Login

Open the production frontend URL.

Login with the demo account.

Expected result:

```txt
User is redirected to dashboard/workspace area.
```

Explain:

```txt
The application uses JWT authentication.
After login, the user can access organization-specific compliance data.
```

---

### Step 2 — Open Workspace

Open the active workspace/organization.

Explain:

```txt
A workspace represents an organization that manages compliance tasks and evidence.
Users can have different roles and permissions inside each workspace.
```

Expected result:

```txt
The dashboard or workspace area loads successfully.
```

---

### Step 3 — Open Evidence Page

Go to:

```txt
Evidence
```

Explain:

```txt
Evidence documents are files or URLs used to prove that a compliance requirement is satisfied.
```

Expected result:

```txt
Evidence list is displayed.
```

---

### Step 4 — Upload New Evidence

Upload a new file, for example:

```txt
Security policy PDF
Risk assessment document
Audit checklist
Vendor compliance document
```

Expected result:

```txt
The file appears as a new evidence card.
```

Technical explanation:

```txt
The frontend asks the backend for a signed upload URL.
The backend creates a Supabase signed upload URL and token.
The frontend uploads the file to Supabase Storage.
Only after the upload succeeds, the frontend creates evidence metadata in the backend.
```

Important:

```txt
Do not use old evidence records created before the Supabase upload flow was fixed.
If an old evidence record shows file-not-found errors, archive it and upload a new file.
```

---

### Step 5 — Confirm File in Supabase Storage

Open Supabase dashboard:

```txt
Storage
→ complipilot-evidence-prod
→ Objects
```

Expected result:

```txt
A new object exists under organizations/{organizationId}/evidence/...
```

Explain:

```txt
Evidence files are stored in a private Supabase Storage bucket.
The application does not expose public file URLs.
```

---

### Step 6 — Download Evidence

Click:

```txt
Download
```

Expected result:

```txt
A temporary signed download URL opens the file.
```

Explain:

```txt
The file bucket is private.
Users do not access files through public URLs.
The backend creates temporary signed URLs for secure access.
```

---

### Step 7 — Run AI Analysis

Click:

```txt
Analyze
```

Expected result:

```txt
AI analysis panel appears.
```

The panel should show:

```txt
Summary
Risk level
Confidence score
Findings
Missing information
Suggested actions
```

Explain:

```txt
The backend sends evidence metadata and linked compliance context to the AI service.
The AI service returns structured analysis.
The result helps users understand whether the evidence is complete and useful.
```

---

### Step 8 — Close AI Panel

Click close on the AI analysis panel.

Expected result:

```txt
The analysis panel is dismissed.
The user can reopen the latest analysis.
```

Explain:

```txt
The UI keeps the evidence list readable while still allowing users to inspect AI results when needed.
```

---

### Step 9 — View AI History

Open:

```txt
AI History
```

Expected result:

```txt
Past analysis results are shown.
```

Explain:

```txt
AI analysis history helps users track previous reviews and changes over time.
```

---

### Step 10 — Link Evidence to Compliance Item

Open compliance item detail and link evidence.

Expected result:

```txt
Evidence is linked to a compliance requirement.
```

Explain:

```txt
This allows the organization to prove that a requirement is supported by actual evidence.
```

---

### Step 11 — Audit Trail

Show audit/history if available.

Explain:

```txt
Important actions are recorded for traceability.
This is important for compliance workflows.
```

---

## 4. Suggested Demo Script

```txt
CompliPilot is an AI Compliance and Evidence OS.

First, I log in to an organization workspace.
Inside the workspace, users can manage compliance items and evidence documents.

Now I open the Evidence page and upload a new evidence file.
The file is stored privately in Supabase Storage.
The backend does not expose public file URLs. Instead, it creates signed upload and download URLs.

After upload, I can download the evidence through a temporary signed URL.

Next, I run AI analysis on this evidence.
The AI service reviews the evidence metadata and compliance context.
It returns a risk level, confidence score, findings, missing information, and suggested actions.

This helps compliance teams quickly understand whether an evidence document is strong enough or whether more information is needed.

Finally, I can view AI analysis history and link the evidence to a compliance item.
This creates a traceable compliance workflow from requirement to evidence to AI review.
```

---

## 5. What to Avoid During Demo

Avoid using old evidence records created before the Supabase upload flow was fixed.

If an old record shows:

```txt
File not found in storage
```

archive it and upload a new file.

Do not expose:

```txt
JWT_SECRET
SUPABASE_SERVICE_ROLE_KEY
DATABASE_PASSWORD
DATABASE_URL with password
```

Do not open environment variable pages during demo unless secrets are hidden.

---

## 6. Quick Health Checks

Backend:

```txt
https://your-backend.onrender.com/actuator/health
```

AI Service:

```txt
https://your-ai-service.onrender.com/health
```

Frontend:

```txt
https://your-frontend.vercel.app
```

Supabase:

```txt
Storage → complipilot-evidence-prod → Objects
```

---

## 7. Demo Success Criteria

The demo is considered successful if:

```txt
User can login
User can open workspace
User can upload evidence
Uploaded file appears in Supabase Storage
User can download evidence
User can run AI analysis
AI analysis panel can be closed/reopened
AI history can be opened
Evidence can be linked to compliance item
Errors are shown in user-friendly messages
```

---

## 8. Troubleshooting During Demo

### Backend is slow on first request

Render free services can be cold-started.

Wait for the backend to wake up and retry.

Check:

```txt
/actuator/health
```

### AI service returns 502

Check:

```txt
AI_SERVICE_BASE_URL
AI service /health
AI service Render logs
```

For demo stability, use rules-based AI fallback.

### Download says file not found

This usually means the evidence metadata exists but the object does not exist in Supabase Storage.

Fix:

```txt
Archive the broken evidence
Upload a new file
```

### Login fails because of CORS

Check backend Render env:

```env
APP_CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app
```

There must be no trailing slash.

Correct:

```txt
https://your-frontend.vercel.app
```

Wrong:

```txt
https://your-frontend.vercel.app/
```

---

## 9. Final Demo Checklist

Before presenting:

```txt
Backend health is UP
AI health is UP
Frontend opens
Demo account works
Supabase bucket exists
At least one new evidence upload/download test passes
AI analysis test passes
Swagger is disabled unless needed
No secrets are visible
```

---

## 10. Recommended Closing Statement

```txt
CompliPilot provides a practical compliance workflow:
organizations can manage requirements, upload private evidence files, link evidence to compliance items, and use AI to review evidence quality.

The system is deployed using a real production-style architecture with Vercel, Render, Neon, and Supabase Storage.
```
