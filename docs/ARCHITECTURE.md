# Architecture

## Decisions
| Topic | Choice |
|---|---|
| SDK | minSdk 26, compileSdk/targetSdk 37 (rev. 1) |
| Language | Kotlin 2.4.x, KSP |
| UI | Jetpack Compose + Material 3, Navigation 3, MVI/UDF per feature |
| Theme | Only `MaterialTheme` color roles (no hard-coded colors) so the system light/dark theme/dynamic color applies |
| DI | Hilt |
| Network | Ktor Client (OkHttp) + kotlinx.serialization (strict, like the server) |
| Local data | Room (source of truth for the UI), DataStore, Keystore/Tink for tokens |
| Sync | WorkManager + outbox, per `../pfm/docs/INTEGRATION.md` §5.6 |
| Locale / currency | vi + en; VND + USD |
| Tests | JUnit5, MockK, Turbine, Compose UI tests |
| Quality | Detekt, Spotless |

## Modules
```
build-logic/                convention plugins
app/                        wiring, navigation host, Application
core:model  core:common     pure Kotlin types / helpers
core:domain                 entities, use cases, repository interfaces (pure Kotlin)
core:data                   repository impls, sync engine
core:database               Room
core:network                Ktor client, DTOs, auth/refresh
core:datastore              preferences + secure token store
core:designsystem           theme, components     core:ui  shared composables
feature:auth | expense | category | habit | budget | report | notification | settings
```
Dependency rule: `feature → domain ← data → (database, network, datastore)`; `domain` never depends on Android.

## Offline-first rules (from the server contract)
- UI reads only Room; only the user and the sync service write to it.
- Every synced row: `id(UUID by app) version deleted dirty localRev failure`; one `cursor` per resource.
- Push order: categories → expenses, habits, budgets; then pull. `410 full_resync_required` → push dirty, reload.
- Money = whole smallest units; dates = `yyyy-MM-dd` in the user's calendar; server timestamps are epoch ms.
- Totals and budget progress are computed locally (port of server `BudgetPeriods`), server reports only when nothing is dirty.
- Server has NO: push (FCM), avatar upload, password change/reset, account deletion, combined `/sync`. Out of scope for v1.
