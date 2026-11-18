# Design — Phase 7: budgets (Penpot page `07 Budgets`)

Status: **waiting for user approval.**

Screens (360x800, light mode, M3 roles only):

1. **Hạn mức** (tab): "Hôm nay" date chip (opens a date picker to look at another day), one card per applicable budget:
   category avatar (or "Tất cả"), period, spent / amount, progress bar, % used and remaining, and a chip
   "Thói quen dự kiến". Status colours: ok = primary, warning = tertiary, exceeded = error. Unsynced budgets carry ⟳. FAB adds a budget.
2. **Empty state** with a button to add the first budget.
3. **Thêm/Sửa hạn mức**: applies to (Tất cả or one category), period chips (Ngày / Tháng / Năm), amount, start date,
   "Lặp lại" switch, optional end date (only when repeating), alert thresholds (default 80% and 100%, up to 5, 1–500%), delete icon when editing.
4. **One-off budget**: repeat switched off; the end date row disappears.

Rules (ported from the server so that the numbers are computed locally, from the local database, including unsynchronized changes):
- Period = the calendar day, month or year that contains the viewed date; a budget applies from the period containing its start date;
  a non-repeating budget only to that period; a repeating one until the period that begins after its end date.
- Spent counts the whole calendar period (also the first one), all expenses or only the budget's category.
- Status: exceeded when spent > amount; warning when a threshold is reached but not exceeded; otherwise ok.
  `percentUsed` = floor(spent * 100 / amount).
- plannedByHabits = for every habit (all of them for an overall budget, only the same category for a category budget):
  amount × the number of its weekdays inside the period.
- Limits: amount 1 … 9 999 999 999 999, at most 100 budgets, `endDate` only with repeating and not before `startDate`.
