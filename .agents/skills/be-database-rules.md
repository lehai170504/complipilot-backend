---
name: be-database-rules
description: Quy tắc thiết kế cơ sở dữ liệu, tối ưu truy vấn SQL, đánh Index và chống lỗi N+1 Query.
---

# Database & JPA Rules

## 1. Query Optimization
- Tuyệt đối tránh lỗi **N+1 Query** khi truy vấn quan hệ (`@OneToMany`, `@ManyToMany`). Sử dụng `JOIN FETCH`, `EntityGraph` hoặc custom SQL DTO projection.
- Mọi bảng bắt buộc phải có các trường audit cơ bản: `created_at`, `updated_at`, `created_by`, `updated_by`.

## 2. Indexing & Naming
- Tên bảng và tên cột dùng chuẩn `snake_case`.
- Khóa ngoại (Foreign Keys) và các cột thường xuyên nằm trong mệnh đề `WHERE`, `JOIN`, `ORDER BY` bắt buộc phải được đánh Index.

## 3. Database Migration
- Không phụ thuộc hoàn toàn vào tự động tạo bảng của ORM (`ddl-auto=update` trong production).
- Mọi thay đổi schema DB phải được viết qua migration script (Flyway / Liquibase) và lưu trong source control.