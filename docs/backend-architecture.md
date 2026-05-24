# Backend Architecture

The backend is a modular monolith: one Spring Boot service, one deployable artifact,
and one PostgreSQL database.

## Modules

All ERP business modules live under `com.school.erp.modules`:

- `auth`
- `users`
- `students`
- `academic`
- `fees`
- `hostel`
- `attendance`
- `reports`
- `notifications`
- `settings`

Each module uses the same internal layers:

```text
api              HTTP controllers, request DTOs, response DTOs
application      use cases, transaction boundaries, orchestration
domain           entities, value objects, domain services, business rules
infrastructure   repositories, persistence adapters, external integrations
```

## Boundary Rules

- Keep all modules in the same Spring Boot application.
- Use one PostgreSQL database for now.
- Keep module tables owned by the module that writes them.
- Put shared primitives in `com.school.erp.common`.
- Do not call another module's repository or infrastructure code directly.
- Prefer module application services, explicit contracts, or internal events for cross-module workflows.
- Keep DTOs in `api`; do not expose JPA entities from controllers.
- Keep transactions in `application`; keep business rules in `domain`.

## Future Extraction

This structure keeps extraction possible later:

- A module's `api` package can become an HTTP contract.
- A module's application service can become a service boundary.
- Module-owned tables can move to a dedicated database later.
- Internal events can become message broker events later.

No microservices are created at this stage.
