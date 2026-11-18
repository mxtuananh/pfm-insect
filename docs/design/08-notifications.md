# Design — Phase 9: notifications (Penpot page `09 Notifications`)

Status: **waiting for user approval.**

Screens (360x800, light mode, M3 roles only):

1. **Trung tâm thông báo** (opened from the bell in the top bar): "Đọc tất cả" action; section "Đang cảnh báo" with the budgets that
   are close to or over their limit right now (computed locally); section "Gần đây" with the alerts of the server grouped by day,
   unread ones highlighted with a dot.
2. **Empty state**.
3. **Banner xin quyền thông báo hệ thống** (Android 13+) and an **offline** banner (the saved alerts are still shown).
4. **Bell with a badge** (number of unread alerts) in the top bar, and the **system notification** the background sync posts
   for a new alert when the app is closed; tapping it opens the notification center.

Behaviour:
- Alerts are structured (`kind` threshold/exceeded, `threshold`, `spent`, `amount`, period, `percentUsed`); the app writes the
  sentence in the language of the user (vi/en). The server creates one alert per budget, period and level.
- Local warnings come from the local database, so a budget can show as exceeded before the server has any alert.
- Reading is local first: tapping an alert or "Đọc tất cả" marks it read on the device (works offline) and the read state is
  pushed in the background (`PUT /v1/notifications/{id}/read`).
- While the app is open the app listens to `GET /v1/notifications/stream` (SSE). On every `ready` and `notification` event it
  pulls `/notifications/changes` to catch up; the stream stops when the app goes to the background and reconnects with back-off.
- No FCM: the server has no push. A background sync (WorkManager) that finds new alerts posts a system notification when the
  permission is granted.
