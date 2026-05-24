# School ERP Admin Panel

Flutter web admin panel for the School ERP modular monolith.

Current demo-ready screens:

- Login and dashboard
- Student Management
- Fees Management
- User Management
- Audit Logs

## Run Locally

Start the backend first, then run:

```powershell
flutter pub get
flutter run -d chrome --web-port=8081 --dart-define=API_BASE_URL=http://localhost:8080/api
```

If Flutter is not on PATH:

```powershell
& "C:\Program Files\flutter\bin\flutter.bat" run -d chrome --web-port=8081 --dart-define=API_BASE_URL=http://localhost:8080/api
```

Full application instructions are in `../docs/run-application.md`.
