# Design — Phase 1: design system + app shell (Penpot file `pfm-insect`)

Status: **waiting for user approval.**

Penpot pages: `01 Design System` (boards Colors, Typography & Shape, Components), `02 App Shell` (board "App shell overview").

- Light mode only is drawn. Implementation must use `MaterialTheme` roles (colorScheme / typography / shapes) only, so the
  app follows the system light/dark theme and dynamic color; the drawn scheme is the fallback (seed teal `#006A60`).
- Font Roboto; M3 type scale; 4dp grid; 16dp screen margin; touch targets >= 48dp.
- Budget status colors: ok = primary, warning = tertiary, exceeded = error.
- App has no income/balance: the headline number is "Tổng chi" (total spent).
- Navigation: Splash -> Auth (not signed in) -> Main with 4 tabs (Chi tiêu, Hạn mức, Báo cáo, Cá nhân).
  Notification bell + sync status in the top bar; Categories, Habits, Notifications, Sync, Settings reached from "Cá nhân";
  add/edit expense from the FAB.
- Splash checks session and `GET /health`; `minAppVersion` greater than the app version blocks with an update screen.
- Icons: Material Symbols (the drawn glyphs are placeholders).

## Revision 2 (user feedback)
- Rounder language: buttons/chips/text fields/banners/snackbar are pills; cards 28-32dp; dialog 40dp; FAB and icons are circles.
  Shape scale: extraSmall 8, small 16, medium 24, large 32, extraLarge 40, full.
- Main screen: **floating bottom bar** (72dp, 16dp margin, pill, surfaceContainer ~72% + 24dp background blur, shadow)
  floating over scrolling content. Blur: RenderEffect/Haze on API 31+, opaque ~92% fallback on API 26-30.
  Content bottom padding >= 120dp; hidden with the keyboard and on child screens.
- Selected indicator fills the whole selected slot (80x64dp pill, secondaryContainer).
- Update-required screen: message centered vertically, primary action (full-width 56dp pill) at the bottom for one-handed use.
