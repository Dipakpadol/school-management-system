# School ERP Demo Testing Checklist

## Login

- Start backend on `http://localhost:8080/api`.
- Start Flutter web on `http://localhost:8081`.
- Login with `admin@school.com` / `Admin@12345678`.
- Confirm the dashboard summary loads without `401`, `403`, or CORS errors.

## Backend Smoke APIs

- `GET /api/v1/dashboard/summary`
- `GET /api/v1/students?size=20`
- `GET /api/v1/users?size=20`
- `GET /api/v1/fees/categories?size=20`
- `GET /api/v1/fees/structures?size=20`
- `GET /api/v1/fees/assignments?size=20`
- `GET /api/v1/audit-logs?size=20&sortBy=performedAt&direction=DESC`

## Admin Panel Smoke Flow

- Students: search, view profile, edit profile, activate/deactivate, export, download template, download profile PDF.
- Users: search/filter, create, edit, activate/deactivate, reset password, delete, export, download template.
- Fees: create category, create/edit fee structure, assign fee to student, collect payment, reverse/void/refund completed payment, find receipt, download receipt PDF, export structures/assignments/defaulters/collection.
- Audit Logs: filter by module/action/user/date and open details.

## Regression Checks

- Login/logout audit events are created.
- Student/user/fee imports return row-wise validation errors when data is invalid.
- Financial records are not physically deleted during reversal, void, or refund.
- Export endpoints download files with authenticated requests.
