# Notification Testing

## Dev / Mock Provider

1. Start the backend with the normal local profile.
2. Log in with a user that has `NOTIFICATIONS_SEND` and `NOTIFICATIONS_READ`.
3. Call the manual test APIs:

```http
POST /api/v1/notifications/test/email
POST /api/v1/notifications/test/sms
POST /api/v1/notifications/test/whatsapp
GET  /api/v1/notifications/logs
```

The current provider is a console/mock provider. It logs the outgoing message and writes a row to `notification_logs`.

## Flutter UI

1. Open `Notification Management`.
2. Use `Send Test` for Email, SMS, and WhatsApp.
3. Open `Logs` and verify channel, recipient, message, status, error, and sent date.
4. Open `Templates` to add, edit, and delete templates.

## Scheduler Testing

Schedulers are present and controlled by:

```yaml
notifications:
  scheduler:
    enabled: true
    fee-due-cron: "0 0/2 * * * *"
    fee-overdue-cron: "0 1/2 * * * *"
    absence-cron: "0 0/2 * * * *"
    exam-reminder-cron: "0 1/2 * * * *"
    hostel-fee-cron: "0 0/2 * * * *"
```

For now, scheduler jobs log execution only. Business queries for due fees, absences, exams, and hostel fee reminders should be added when those event contracts are finalized.

## Production Provider Setup

Do not put provider secrets in Flutter or source code. Configure them through environment variables or deployment secrets.

- Email: SMTP, SendGrid, or AWS SES credentials.
- SMS: Twilio, MSG91, Textlocal, or Fast2SMS API key.
- WhatsApp: WhatsApp Business Cloud API, Gupshup, or Twilio WhatsApp token.

WhatsApp production messages may require approved templates depending on provider and country rules.
