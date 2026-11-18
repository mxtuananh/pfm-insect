# Design — Phase 6: habits (Penpot page `06 Habits`)

Status: **waiting for user approval.**

A habit is a template (title, note, amount, category, days of the week, optional time of day, sort order). The server never
creates expenses from habits; the app copies the habit into an ordinary expense when the user asks.

Screens (360x800, light mode, M3 roles only):

1. **Thói quen** (from Cá nhân): rows with category avatar, title, days summary + time, amount and a round "＋" quick-add
   button; extended FAB "＋ Thói quen". Habits that fall on today come first and are highlighted. Rows can be moved up/down
   from a menu (sort order). Empty state with a button to add the first habit.
2. **Quick add**: tapping "＋" creates an expense dated today (title, note, amount, category of the habit) at once,
   works offline, and shows a snackbar "Đã thêm … · HOÀN TÁC".
3. **Thêm/Sửa thói quen**: amount, name, category chips, seven day toggles (T2…CN), optional time, optional note, delete icon when editing.
4. **Thêm chi tiêu**: a row "Từ thói quen" of chips prefills the form (still editable before saving).

Rules: at most 200 habits (server limit); days are ISO 1 = Monday … 7 = Sunday; time is "HH:mm" and only used for display and
ordering (no push notifications: the server has none).
