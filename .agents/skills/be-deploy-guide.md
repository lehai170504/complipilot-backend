---
name: be-deploy-guide
description: Hướng dẫn đóng gói Docker container, kiểm tra cấu hình môi trường và quy trình deploy Backend.
---

# Backend Deployment Guide

## 1. Environment Verification
- Trước khi build/deploy, luôn kiểm tra các biến môi trường cấu hình (Database URL, Credentials, JWT Secrets, Redis, Mail Server).
- Không được hardcode bất kỳ secret key nào trong code. Tất cả phải load qua `.env` hoặc Environment Variables.

## 2. Build & Testing Steps
- Chạy Unit Test và Integration Test trước khi đóng gói:
  - Spring Boot: `./mvnw clean test` hoặc `./gradlew test`
  - Node.js: `npm run test`
- Build file thực thi (JAR/Artifact) và đảm bảo không bị dính warning/error đòn gánh.

## 3. Dockerization
- Sử dụng Multi-stage Build trong `Dockerfile` để giảm tối đa kích thước Image.
- Container phải chạy dưới quyền user phi quản trị (Non-root user) để đảm bảo bảo mật.
- Cấu hình Healthcheck endpoint (`/actuator/health` hoặc `/health`) để Load Balancer/K8s nhận biết trạng thái app.