# Audit Log API

Audit logs are available to users with `AUDIT_LOGS_READ`.

## Endpoints

```text
GET /api/v1/audit-logs
GET /api/v1/audit-logs/{id}
GET /api/v1/audit-logs/by-module/{moduleName}
GET /api/v1/audit-logs/by-entity/{entityName}/{entityId}
```

## Filters

`GET /api/v1/audit-logs` supports:

- `query`
- `moduleName`
- `entityName`
- `entityId`
- `action`
- `performedBy`
- `performedFrom`
- `performedTo`
- `page`
- `size`
- `sortBy`
- `direction`

Default sorting is `performedAt DESC`.

## Sample Requests

Login:

```powershell
$login = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body (@{
    email = "admin@school.test"
    password = "Admin@12345678"
  } | ConvertTo-Json)

$token = $login.data.accessToken
```

List latest audit logs:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/v1/audit-logs?size=20" `
  -Headers @{ Authorization = "Bearer $token" }
```

Filter by module and action:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/v1/audit-logs?moduleName=FEES&action=PAYMENT_COLLECTED&size=20" `
  -Headers @{ Authorization = "Bearer $token" }
```

Filter by entity:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/v1/audit-logs/by-entity/Student/{studentId}" `
  -Headers @{ Authorization = "Bearer $token" }
```
