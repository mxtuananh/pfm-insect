# Plan

Workflow per module: **design in Penpot -> user approval -> implement -> test on device -> local commit.**
Design is light mode only; colors use Material3 `MaterialTheme` roles, so the app follows the system theme.

| # | Phase | Design (Penpot, wait for approval) | Implementation |
|---|---|---|---|
| 0 | Repo, docs, server | - | git init, docs, dev server verified (done) |
| 1 | Design system + app shell | tokens/roles, typography, shared components, navigation shell | Gradle skeleton, `build-logic`, `core:designsystem`, `core:ui`, app shell |
| 2 | Core platform | - | `core:network`, `core:database`, `core:datastore`, `core:common/model/domain`, health + `minAppVersion` |
| 3 | `feature:auth` + profile | login, register, session expired, profile | auth, deviceId, serialized refresh, profile (timezone/currency) |
| 4 | Sync engine | - (sync status indicators only) | outbox, pull/cursor, `410`, WorkManager |
| 5 | `feature:category` + `feature:expense` | expense list/add/edit, filters, category management | MVP core flow, usable offline |
| 6 | `feature:habit` | habit list/edit, quick-add | CRUD + one-tap expense |
| 7 | `feature:budget` | budget list/edit/progress | local `BudgetPeriods` port, progress, `plannedByHabits` |
| 8 | `feature:report` | summary, by-category charts | local aggregates |
| 9 | `feature:notification` | notification center | SSE, catch-up via `/changes`, local warnings |
| 10 | `feature:settings` + polish | settings, empty/error states | i18n (vi/en), a11y, tablets |
| 11 | Quality and release | - | tests, R8, baseline profile, security review, signing, AAB |

MVP = phases 0-5. Out of scope (server limits): FCM push, avatar upload, password reset/change, account deletion.
No CI/CD for now. Git is local only (never push).
