package hopper.pfm.insect.feature.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hopper.pfm.insect.core.common.Money
import hopper.pfm.insect.core.domain.report.CategoryShare
import hopper.pfm.insect.core.domain.report.Report
import hopper.pfm.insect.core.domain.report.ReportKind
import hopper.pfm.insect.core.domain.report.TrendBucket
import hopper.pfm.insect.core.domain.report.TrendUnit
import hopper.pfm.insect.core.model.Category
import hopper.pfm.insect.core.model.Expense
import hopper.pfm.insect.core.ui.CategoryAvatar
import hopper.pfm.insect.core.ui.CategoryColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private fun LocalDate.toPickerMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toPickerDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
private val DayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
private val DayMonthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Content of the Báo cáo tab (the shell draws the top bar and the floating bar). */
@Composable
fun ReportScreen(
    onCategory: (categoryId: String?, from: String, to: String) -> Unit,
    bottomBarSpace: Dp,
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf<String?>(null) } // "from" or "to"
    val report = state.report

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "switch") { KindSwitcher(state.kind, viewModel::setKind) }
        item(key = "range") {
            when (state.kind) {
                ReportKind.MONTH -> Navigator(
                    label = stringResource(R.string.report_month_label, state.month.format(DateTimeFormatter.ofPattern("MM/yyyy"))),
                    onPrevious = { viewModel.shift(-1) },
                    onNext = { viewModel.shift(1) },
                )
                ReportKind.YEAR -> Navigator(
                    label = stringResource(R.string.report_year_label, state.year),
                    onPrevious = { viewModel.shift(-1) },
                    onNext = { viewModel.shift(1) },
                )
                ReportKind.CUSTOM -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DateChip(stringResource(R.string.report_from), state.customFrom.format(DayMonthYear), Modifier.weight(1f)) { picking = "from" }
                    DateChip(stringResource(R.string.report_to), state.customTo.format(DayMonthYear), Modifier.weight(1f)) { picking = "to" }
                }
            }
        }
        state.problem?.let { problem ->
            item(key = "problem") {
                Text(
                    stringResource(if (problem == RangeProblem.FromAfterTo) R.string.report_error_from_after_to else R.string.report_error_too_long),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
        if (report != null) {
            item(key = "summary") { SummaryCard(report, state.kind, state.currency) }
            item(key = "tiles") { Tiles(report, state.kind, state.currency) }
            if (report.total <= 0) {
                item(key = "empty") { EmptyReport() }
            } else {
                item(key = "trend") { TrendCard(report, state.currency, state.today) }
                item(key = "categories") {
                    CategoryCard(
                        shares = report.categories,
                        categories = state.categories,
                        currency = state.currency,
                        onClick = { share -> onCategory(share.categoryId, report.range.start.toString(), report.range.end.toString()) },
                    )
                }
            }
        }
    }

    picking?.let { which ->
        val initial = if (which == "from") state.customFrom else state.customTo
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = millis.toPickerDate()
                        if (which == "from") viewModel.setCustomFrom(date) else viewModel.setCustomTo(date)
                    }
                    picking = null
                }) { Text(stringResource(R.string.report_ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text(stringResource(R.string.report_cancel)) } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun KindSwitcher(selected: ReportKind, onSelect: (ReportKind) -> Unit) {
    val options = listOf(
        ReportKind.MONTH to R.string.report_kind_month,
        ReportKind.YEAR to R.string.report_kind_year,
        ReportKind.CUSTOM to R.string.report_kind_custom,
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (kind, label) ->
            SegmentedButton(
                selected = selected == kind,
                onClick = { onSelect(kind) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun Navigator(label: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.report_previous))
        }
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.report_next))
        }
    }
}

@Composable
private fun DateChip(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryCard(report: Report, kind: ReportKind, currency: String) {
    val unit = stringResource(
        when (kind) {
            ReportKind.MONTH -> R.string.report_unit_month
            ReportKind.YEAR -> R.string.report_unit_year
            ReportKind.CUSTOM -> R.string.report_unit_period
        },
    )
    Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.report_total), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(Money.formatWithSymbol(report.total, currency), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            val delta = report.deltaPercent
            val text = when {
                delta == null -> null
                delta > 0 -> stringResource(R.string.report_delta_up, delta, unit)
                delta < 0 -> stringResource(R.string.report_delta_down, -delta, unit)
                else -> stringResource(R.string.report_delta_same, unit)
            }
            if (text != null) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)) {
                    Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
            if (kind == ReportKind.CUSTOM) {
                Text(
                    stringResource(R.string.report_days_count, report.range.days, report.count),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun bucketLabel(bucket: TrendBucket, unit: TrendUnit): String = when (unit) {
    TrendUnit.DAY -> bucket.start.format(DayMonth)
    TrendUnit.WEEK -> bucket.start.format(DayMonth) + "–" + bucket.end.format(DayMonth)
    TrendUnit.MONTH -> bucket.start.format(DateTimeFormatter.ofPattern("MM/yyyy"))
}

@Composable
private fun Tiles(report: Report, kind: ReportKind, currency: String) {
    val peak = report.peak
    val peakTitle = when (report.trendUnit) {
        TrendUnit.DAY -> R.string.report_peak_day
        TrendUnit.WEEK -> R.string.report_peak_week
        TrendUnit.MONTH -> R.string.report_peak_month
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(stringResource(R.string.report_count), report.count.toString(), Modifier.weight(1f))
        if (kind == ReportKind.YEAR) {
            Tile(stringResource(R.string.report_avg_month), Money.formatWithSymbol(report.averagePerMonth, currency), Modifier.weight(1.4f))
        } else {
            Tile(stringResource(R.string.report_avg_day), Money.formatWithSymbol(report.averagePerDay, currency), Modifier.weight(1.4f))
        }
        Tile(stringResource(peakTitle), peak?.let { bucketLabel(it, report.trendUnit) } ?: stringResource(R.string.report_none), Modifier.weight(1.2f))
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyReport() {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.report_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.report_empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Bars of the trend (day, week or month). Tapping a bar shows its amount; buckets after today are greyed. */
@Composable
private fun TrendCard(report: Report, currency: String, today: LocalDate) {
    val colors = MaterialTheme.colorScheme
    val trend = report.trend
    val max = trend.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1L
    var selected by remember(report.range) { mutableStateOf<Int?>(null) }
    Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(
                        when (report.trendUnit) {
                            TrendUnit.DAY -> R.string.report_trend_day
                            TrendUnit.WEEK -> R.string.report_trend_week
                            TrendUnit.MONTH -> R.string.report_trend_month
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                )
                selected?.let { index ->
                    val bucket = trend[index]
                    Text(
                        bucketLabel(bucket, report.trendUnit) + " · " + Money.formatWithSymbol(bucket.amount, currency),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                    )
                }
            }
            val barColor = colors.primaryContainer
            val selectedColor = colors.primary
            val emptyColor = colors.outlineVariant
            val peakIndex = report.peak?.let { peak -> trend.indexOfFirst { it.start == peak.start } }
            Canvas(
                Modifier.fillMaxWidth().height(120.dp).pointerInput(trend) {
                    detectTapGestures { offset ->
                        val index = (offset.x / (size.width / trend.size)).toInt().coerceIn(0, trend.size - 1)
                        selected = if (selected == index) null else index
                    }
                },
            ) {
                val slot = size.width / trend.size
                val gap = slot * 0.25f
                val barWidth = slot - gap
                trend.forEachIndexed { i, bucket ->
                    val height = if (bucket.amount <= 0) 3.dp.toPx() else (size.height * bucket.amount / max).toFloat().coerceAtLeast(6.dp.toPx())
                    val color = when {
                        i == (selected ?: peakIndex) -> selectedColor
                        bucket.amount <= 0 -> emptyColor
                        else -> barColor
                    }
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(i * slot + gap / 2, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
                    )
                }
            }
            when (report.trendUnit) {
                TrendUnit.MONTH -> Row(Modifier.fillMaxWidth()) {
                    trend.forEach { bucket ->
                        Text(
                            bucket.start.monthValue.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (bucket.start.isAfter(today)) colors.outline else colors.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                TrendUnit.DAY -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(trend.first(), trend[trend.size / 2], trend.last()).forEach {
                        Text(it.start.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
                TrendUnit.WEEK -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(trend.first().start.format(DayMonth), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Text(trend.last().end.format(DayMonth), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(shares: List<CategoryShare>, categories: Map<String, Category>, currency: String, onClick: (CategoryShare) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.report_by_category), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            // The share bar: one segment per row, as wide as its amount.
            Row(Modifier.fillMaxWidth().height(14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                shares.forEach { share ->
                    val category = share.categoryId?.let { categories[it] }
                    Box(
                        Modifier.weight(share.amount.toFloat().coerceAtLeast(1f)).height(14.dp)
                            .background(CategoryColors.parse(category?.color ?: CategoryColors.DEFAULT), CircleShape),
                    )
                }
            }
            shares.forEach { share ->
                val category = share.categoryId?.let { categories[it] }
                val merged = share.mergedCategories > 0
                val name = when {
                    merged -> stringResource(R.string.report_other_merged, share.mergedCategories)
                    category == null -> stringResource(R.string.report_other)
                    else -> category.name
                }
                Row(
                    Modifier.fillMaxWidth().then(if (merged) Modifier else Modifier.clickable { onClick(share) }).padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(12.dp).background(CategoryColors.parse(category?.color ?: CategoryColors.DEFAULT), CircleShape))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(R.string.report_category_row, share.percent, share.count),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Text(Money.formatWithSymbol(share.amount, currency), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

/** The expenses of one category in the range of the report, grouped by day. */
@Composable
fun ReportCategoryScreen(
    categoryId: String?,
    from: String,
    to: String,
    onBack: () -> Unit,
    onEdit: (expenseId: String) -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel<CategoryDetailViewModel, CategoryDetailViewModel.Factory>(
        key = "report-category-$categoryId-$from-$to",
        creationCallback = { factory -> factory.create(categoryId, from, to) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val title = state.category?.name ?: stringResource(R.string.report_other)
    Surface(modifier = Modifier.fillMaxSize(), color = colors.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.report_back))
                }
                Text(
                    title + " · " + LocalDate.parse(from).format(DayMonth) + "–" + LocalDate.parse(to).format(DayMonthYear),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "summary") {
                    Surface(shape = RoundedCornerShape(28.dp), color = colors.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Bottom) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.report_total), style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
                                Text(Money.formatWithSymbol(state.total, state.currency), style = MaterialTheme.typography.headlineMedium, color = colors.onPrimaryContainer)
                            }
                            val percent = state.rangeTotal.takeIf { it > 0 }?.let { (state.total * 100 / it).toInt() } ?: 0
                            Text(
                                stringResource(R.string.report_detail_summary, state.count, percent),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onPrimaryContainer,
                            )
                        }
                    }
                }
                state.groups.forEach { group ->
                    item(key = "h-${group.date}") {
                        val name = when (group.date) {
                            state.today -> stringResource(R.string.report_today)
                            state.today.minusDays(1) -> stringResource(R.string.report_yesterday)
                            else -> group.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$name · " + group.date.format(DayMonth), style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
                            Text(Money.formatWithSymbol(group.total, state.currency), style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
                        }
                    }
                    items(group.items, key = { it.id }) { expense ->
                        DetailRow(expense, state.category, state.currency) { onEdit(expense.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(expense: Expense, category: Category?, currency: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryAvatar(category?.icon, category?.color)
            Column(Modifier.weight(1f)) {
                Text(expense.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                category?.let { Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text("−" + Money.formatWithSymbol(expense.amount, currency), style = MaterialTheme.typography.titleMedium)
        }
    }
}
