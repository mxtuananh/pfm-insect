# Design — Phase 8: reports (Penpot page `08 Reports`)

Status: **waiting for user approval.**

Screens (360x800, light mode, M3 roles only), the "Báo cáo" tab:

1. **Tháng**: segmented switch (Tháng / Năm / Tùy chọn), `‹ Tháng 11/2026 ›`, summary card (total and the change against the
   previous month), three stat tiles (number of expenses, average per day, busiest day), a day-by-day bar chart and
   "Theo danh mục" (a share bar and a list ordered by amount; small categories are merged into "Khác").
2. **Năm**: same layout, twelve monthly bars (future months greyed), tiles with the average per month and the busiest month.
3. **Tùy chọn**: two date fields (from / to, at most 5 years); the trend groups by day, week or month depending on the length.
4. **Chi tiết danh mục**: tapping a category row lists that category's expenses of the period, grouped by day.

Rules:
- Everything is computed locally from Room (including expenses that are not synchronized yet), never from `/v1/reports/*`,
  so reports work offline and match the other screens. Server numbers are only a cross-check when the outbox is empty.
- Charts are drawn with Compose Canvas (no chart library); tapping a bar shows its amount.
- No income and no balance; the currency comes from the profile.
