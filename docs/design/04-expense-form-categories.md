# Design — Phase 5: expense form + categories (Penpot page `05 Expense form & Categories`)

Status: **waiting for user approval.** The expense list/chart itself was approved earlier (`03 Expense (Chi tiêu)`).

Screens (360x800, light mode, M3 roles only):

1. **Thêm chi tiêu** — big amount field (numeric keypad, thousands separators, currency symbol of the profile), title,
   category chips (+ "＋ Danh mục" opens the category editor), date (M3 date picker, default today), optional note, pill "Lưu".
2. **Sửa chi tiêu** — same form; trash icon in the top bar (also swipe in the list); a banner "Chưa đồng bộ" while the record is dirty.
   A server rejection (`failure`) shows a red banner with the reason; editing sends the record again.
3. **Lỗi nhập** — amount must be > 0 and at most 9 999 999 999 999, title 1–100 characters, note up to 500; "Lưu" is disabled until valid.
4. **Danh mục** — list with icon + colour, "Mặc định" / own categories, a collapsible "Đã lưu trữ" section, extended FAB "＋ Danh mục".
5. **Sửa danh mục** — name (1–50), icon grid, colour swatches, archive switch. Categories are never deleted, only archived
   (archived ones still show on old expenses). Default categories can be edited.

Behaviour:
- Every save writes the local database at once (dirty + `localRev`), then asks the sync service to run; the UI never waits for the network.
- Delete sets the tombstone and shows a snackbar with "Hoàn tác" (undo restores the record and keeps it dirty).
- Removing a category is not possible; archiving hides it from the chips for new expenses.
- Habits (quick add) arrive in Phase 6; the form already leaves room for a "Thói quen" shortcut row.
