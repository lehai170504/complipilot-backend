---
name: be-coding-standards
description: Quy chuẩn viết code Backend, thiết kế RESTful API, quản lý Exception và Transaction.
---

# Backend Coding Standards

## 1. RESTful API Naming & Structure
- Endpoint URLs phải dùng danh từ số nhiều, chữ thường, cách nhau bởi dấu gạch ngang (`/api/v1/organization-members`).
- Sử dụng đúng HTTP Method: `GET` (lấy dữ liệu), `POST` (tạo mới), `PUT/PATCH` (cập nhật), `DELETE` (xóa).
- Mọi API response đều phải bọc trong một chuẩn DTO cố định (Data Transfer Object) chứa: `status`, `message`, `data`, `timestamp`.
- **Phạm vi áp dụng:** quy tắc bọc response ở trên CHỈ áp dụng cho endpoint viết mới từ 22/10/2026 trở đi; toàn bộ endpoint hiện có giữ nguyên, KHÔNG refactor. Các endpoint sau không bọc: Stripe webhook, export CSV, health probe, và response `204 No Content`.

## 2. Architecture Layering
Code phải tuân thủ nghiêm ngặt mô hình phân tầng:
- **Controller Layer:** Chỉ tiếp nhận request, validate dữ liệu đầu vào và gọi Service. Không xử lý business logic ở đây.
- **Service Layer:** Chứa toàn bộ Business Logic. 
- **Repository/DAO Layer:** Chỉ làm việc với cơ sở dữ liệu.

## 3. Transaction Management
- Mọi phương thức làm thay đổi dữ liệu trên nhiều bảng (Multi-Write / Multi-Table Operations) BẮT BUỘC phải bọc trong `@Transactional` để đảm bảo tính nguyên tố (Atomicity).
- Tránh gọi method nội bộ `@Transactional` (Self-Invocation) trong cùng một class vì sẽ làm mất AOP Proxy.

## 4. Exception Handling
- Sử dụng Global Exception Handler để bắt và trả về JSON Error Response chuẩn.
- Không nuốt lỗi (`catch (Exception e) {}`). Luôn log error rõ ràng kèm context.