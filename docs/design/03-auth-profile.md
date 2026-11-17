# Design — Phase 3: auth + profile (Penpot page `04 Auth & Profile`)

Status: **waiting for user approval.**

Screens (360x800, light mode, M3 roles only):

1. **Đăng nhập** — email, password (show/hide), pill "Đăng nhập", link to register. The app sends the `deviceId` of the installation.
2. **Đăng ký** — email, password, confirm password, live checklist of the password rules (>= 8 characters, lowercase + uppercase,
   digit; max 72 bytes). Server error `email_taken` is shown under the email field. After success the app signs in automatically.
3. **Phiên hết hạn** — shown when the refresh token is rejected. Local data is kept; the email is fixed, only the password is asked.
   "Dùng tài khoản khác" wipes local data after a confirmation dialog.
4. **Tab Cá nhân** — profile card (initial avatar, name, email, timezone and currency chips, "Sửa"), then Danh mục, Thói quen,
   Thông báo, Đồng bộ, Cài đặt, Đăng xuất. Sign-out asks "this device" or "all devices" and warns when changes are unsynced.
5. **Sửa hồ sơ** — name (1-50), email (read-only), timezone (IANA picker), currency chips VND/USD. Currency is locked when the
   user already has expenses, habits or budgets (`currency_locked`).

Not in scope (server limits): avatar upload, change email/password, password reset, delete account.
Errors map from codes: `invalid_credentials`, `email_taken`, `validation_failed`, `rate_limited` (wait a minute), offline/timeouts.
