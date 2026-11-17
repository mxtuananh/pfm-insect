# Design — Expense screen (Penpot page `03 Expense (Chi tiêu)`)

Status: **waiting for user approval.**

Decisions (user confirmed): periods Ngày / Tuần / Tháng on the Expense tab, default **Tháng**; the Năm period lives in the Report tab.

- Layout top to bottom: top bar (sync + bell) · period switcher (pill segmented) · period navigator `‹ label ›` ·
  summary card (total + delta vs previous period) · chart card · category chips (filter chart + list) · list grouped by day
  with daily totals · FAB (circle) · floating bottom bar.
- Month: 30 daily bars, selected day = primary + tooltip; tap a bar scrolls the list to that day.
- Week (starts Monday, changeable in Settings): 7 bars T2..CN.
- Day: expenses have a date only (`spentOn`), no time, so a stacked category breakdown replaces the bar chart.
- Empty state: total 0, flat chart, CTA "Thêm khoản chi".
- All numbers are computed locally from Room (include unsynced items, marked with a sync glyph), per INTEGRATION.md §5.5.
- Bars drawn with Compose Canvas (no chart library).
