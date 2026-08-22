package com.example.presentation.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.presentation.components.MonthYearSelectionBottomSheet
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CashTransaction
import com.example.domain.model.PaymentMethod
import com.example.domain.model.TransactionType
import com.example.presentation.components.TransactionEditBottomSheet
import com.example.presentation.components.TransactionViewBottomSheet
import com.example.presentation.theme.LaborBackground
import com.example.presentation.theme.LaborBlue
import com.example.presentation.theme.LaborBlueLight
import com.example.presentation.theme.LaborDivider
import com.example.presentation.theme.LaborError
import com.example.presentation.theme.LaborSuccess
import com.example.presentation.theme.LaborTextHint
import com.example.presentation.theme.LaborTextPrimary
import com.example.presentation.theme.LaborTextSecondary
import com.example.presentation.viewmodel.LaborViewModel
import com.example.presentation.viewmodel.Screen
import com.example.core.util.AppStrings
import com.example.core.util.LaborCalendarHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CashBookScreen(
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val searchQuery by viewModel.transactionSearchQuery.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val lang = userProfile.language

    val activeTransaction by viewModel.activeTransaction.collectAsStateWithLifecycle()
    val sheetMode by viewModel.transactionSheetMode.collectAsStateWithLifecycle()

    var showMonthDialog by remember { mutableStateOf(false) }

    // Filter transactions based on selected month if specific month selected
    val displayTransactions = remember(transactions, selectedMonth) {
        val nonZeroList = transactions.filter { it.amount > 0.0 }
        if (selectedMonth == "All Months" || selectedMonth.isBlank()) {
            nonZeroList
        } else {
            // E.g. "Aug 2026" matches "2026-08" or "Aug 2026"
            // Note: fullDate is expected in "yyyy-MM-dd" format. Any matching logic must
            // anchor on both year and month together, never month alone.
            // Example: Filtering by "Aug 2026" matches "2026-08-15", but must NOT match "2025-08-15".
            val monthMap = mapOf(
                "Jan" to "01", "Feb" to "02", "Mar" to "03", "Apr" to "04",
                "May" to "05", "Jun" to "06", "Jul" to "07", "Aug" to "08",
                "Sep" to "09", "Oct" to "10", "Nov" to "11", "Dec" to "12"
            )
            val monthPart = selectedMonth.take(3)
            val monthNum = monthMap[monthPart]
            val parts = selectedMonth.split(" ")
            val yearPart = if (parts.size >= 2) parts[1] else "2026"

            nonZeroList.filter { tx ->
                val matchFullDate = if (monthNum != null && yearPart.isNotBlank()) {
                    tx.fullDate.startsWith("$yearPart-$monthNum")
                } else false

                val matchDisplayDate = tx.dateDisplay.contains(monthPart, ignoreCase = true) &&
                        (yearPart.isBlank() || tx.dateDisplay.contains(yearPart) || tx.fullDate.startsWith(yearPart))

                matchFullDate || matchDisplayDate
            }
        }
    }

    val cashInTotal = remember(displayTransactions) { displayTransactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount } }
    val cashOutTotal = remember(displayTransactions) { displayTransactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount } }
    val balance = cashInTotal - cashOutTotal

    val monthsList = remember(allTransactions) {
        val currentYear = LaborCalendarHelper.todayYear
        val currentMonth = LaborCalendarHelper.todayMonth

        // 1. Rolling window of the last 12 months up to and including current month
        val rollingMonths = mutableListOf<Pair<Int, Int>>()
        var y = currentYear
        var m = currentMonth
        for (i in 0 until 12) {
            rollingMonths.add(Pair(y, m))
            m -= 1
            if (m < 1) {
                m = 12
                y -= 1
            }
        }

        // 2. Distinct year-months present in transactions (historical fallback)
        val transactionYearMonths = mutableSetOf<Pair<Int, Int>>()
        for (tx in allTransactions) {
            if (tx.fullDate.isNotBlank()) {
                val parsed = LaborCalendarHelper.parseYearMonth(tx.fullDate)
                transactionYearMonths.add(parsed)
            } else if (tx.dateDisplay.isNotBlank()) {
                val parsed = LaborCalendarHelper.parseYearMonth(tx.dateDisplay)
                transactionYearMonths.add(parsed)
            } else if (tx.timestamp > 0) {
                val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                transactionYearMonths.add(Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1))
            }
        }

        // 3. Merge, sort descending by date, remove duplicates
        val allYearMonths = (rollingMonths + transactionYearMonths).distinct().sortedWith { a, b ->
            if (a.first != b.first) b.first.compareTo(a.first)
            else b.second.compareTo(a.second)
        }

        // 4. Format to "MMM yyyy" and keep "All Months" as the last entry
        allYearMonths.map { (year, month) ->
            LaborCalendarHelper.formatYearMonth(year, month)
        } + "All Months"
    }

    // Calendar Picker Dialog
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, _ ->
            val cal = Calendar.getInstance()
            cal.set(year, month, 1)
            val monthStr = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
            viewModel.updateSelectedMonth(monthStr)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.White,
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = AppStrings.get("cash_book", lang),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        // Month Selector Pill (Calendar icon + Month + chevron)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                            modifier = Modifier
                                .clickable { showMonthDialog = true }
                                .testTag("cashbook_month_selector_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Select Calendar Month",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedMonth,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // Bottom Action Buttons: '+ CASH IN' and '- CASH OUT'
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 0.dp
                ) {
                    Column {
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // '+ CASH IN'
                            Button(
                                onClick = { viewModel.openNewTransaction(TransactionType.CASH_IN) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_cash_in"),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00875A))
                            ) {
                                Text(
                                    text = "+ ${AppStrings.get("cash_in", lang).uppercase()}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // '- CASH OUT'
                            Button(
                                onClick = { viewModel.openNewTransaction(TransactionType.CASH_OUT) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_cash_out"),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                            ) {
                                Text(
                                    text = "- ${AppStrings.get("cash_out", lang).uppercase()}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onTransactionSearchQueryChanged(it) },
                        placeholder = { Text(AppStrings.get("search_transactions", lang), color = Color(0xFF8E8E93), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF1D61E7),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("tx_search_input"),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E293B),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                item { Spacer(modifier = Modifier.height(2.dp)) }

                // Summary Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.2.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Row 1: Cash In
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.get("cash_in_label", lang),
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "₹ ${cashInTotal.toInt()}",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669) // Green
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Row 2: Cash Out
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.get("cash_out_label", lang),
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "₹ ${cashOutTotal.toInt()}",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626) // Red
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Row 3: Balance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.get("balance", lang),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "₹ ${balance.toInt()}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (balance >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 'View Report' light blue pill button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .clickable { viewModel.navigateTo(Screen.CashBookReport) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF1D61E7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = AppStrings.get("view_report", lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D61E7)
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }

                // Transaction List / Empty State
                if (displayTransactions.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = AppStrings.get("no_transactions", lang),
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                } else {
                    // Entire Structured Compact Table matching reference screenshot
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Table Header Row with Vertical Dividers (Excel grid look)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFAFAFA))
                                        .height(IntrinsicSize.Min),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = AppStrings.get("date", lang),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827),
                                        modifier = Modifier
                                            .weight(0.20f)
                                            .padding(vertical = 10.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    VerticalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())
                                    Text(
                                        text = AppStrings.get("notes", lang),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827),
                                        modifier = Modifier
                                            .weight(0.44f)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        textAlign = TextAlign.Start
                                    )
                                    VerticalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())
                                    Text(
                                        text = "₹ " + AppStrings.get("amount", lang),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827),
                                        modifier = Modifier
                                            .weight(0.36f)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        textAlign = TextAlign.Start
                                    )
                                }

                                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)

                                // Table Items with Vertical Grid Dividers (Excel look)
                                displayTransactions.forEachIndexed { index, tx ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.openTransactionDetail(tx) }
                                            .testTag("tx_row_${tx.id}")
                                            .height(IntrinsicSize.Min),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val (dayNum, dayName) = parseDayAndWeek(tx.fullDate, tx.dateDisplay, tx.timestamp)

                                        // Date Column (Stacked Day Number & Day Name, Centered)
                                        Column(
                                            modifier = Modifier
                                                .weight(0.20f)
                                                .padding(vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNum,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF111827)
                                            )
                                            Text(
                                                text = dayName,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF6B7280)
                                            )
                                        }

                                        VerticalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())

                                        // Notes & Payment Method Column
                                        Column(
                                            modifier = Modifier
                                                .weight(0.44f)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = tx.notes.ifBlank { "-" },
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF111827),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = tx.paymentMethod.name.uppercase(),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF6B7280)
                                            )
                                        }

                                        VerticalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())

                                        // Amount Column: Left aligned Amount with right aligned chevron
                                        Row(
                                            modifier = Modifier
                                                .weight(0.36f)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val formattedAmount = if (tx.amount % 1.0 == 0.0) {
                                                "${tx.amount.toInt()}"
                                            } else {
                                                "${tx.amount}"
                                            }
                                            Text(
                                                text = "₹$formattedAmount",
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tx.type == TransactionType.CASH_IN) Color(0xFF16A34A) else Color(0xFFDC2626)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = Color(0xFFD1D5DB),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    if (index < displayTransactions.size - 1) {
                                        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        // Calendar Month Selection Bottom Sheet
        if (showMonthDialog) {
            MonthYearSelectionBottomSheet(
                initialSelection = selectedMonth,
                onDismiss = { showMonthDialog = false },
                onConfirm = { newSelection -> 
                    viewModel.updateSelectedMonth(newSelection)
                    showMonthDialog = false 
                }
            )
        }

        // Transaction Detail Modals
        when (sheetMode) {
            LaborViewModel.TransactionSheetMode.VIEW -> {
                if (activeTransaction != null) {
                    TransactionViewBottomSheet(
                        transaction = activeTransaction!!,
                        onEditClick = { viewModel.openTransactionEdit(activeTransaction!!) },
                        onCloseClick = { viewModel.closeTransactionSheet() }
                    )
                }
            }
            LaborViewModel.TransactionSheetMode.EDIT,
            LaborViewModel.TransactionSheetMode.CREATE_IN,
            LaborViewModel.TransactionSheetMode.CREATE_OUT -> {
                val defaultType = if (sheetMode == LaborViewModel.TransactionSheetMode.CREATE_OUT) TransactionType.CASH_OUT else TransactionType.CASH_IN
                TransactionEditBottomSheet(
                    transaction = if (sheetMode == LaborViewModel.TransactionSheetMode.EDIT) activeTransaction else null,
                    defaultType = defaultType,
                    onSave = { id, amount, method, notes, type, dateDisplay, fullDate ->
                        viewModel.saveTransaction(id, amount, method, notes, type, dateDisplay, fullDate)
                    },
                    onDelete = { id ->
                        viewModel.deleteTransaction(id)
                    },
                    onClose = { viewModel.closeTransactionSheet() }
                )
            }
            null -> {}
        }
    }
}

private fun parseDayAndWeek(fullDate: String, dateDisplay: String, timestamp: Long): Pair<String, String> {
    if (fullDate.isNotBlank()) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fullDate)
            if (date != null) {
                val dayNum = SimpleDateFormat("d", Locale.getDefault()).format(date)
                val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
                return Pair(dayNum, dayName)
            }
        } catch (e: Exception) {
            // fallback
        }
    }
    if (dateDisplay.isNotBlank()) {
        val parts = dateDisplay.trim().split(" ")
        if (parts.isNotEmpty()) {
            val dayNum = parts[0]
            val dayName = if (parts.size > 1) parts[1] else ""
            return Pair(dayNum, dayName)
        }
    }
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val dayNum = SimpleDateFormat("d", Locale.getDefault()).format(cal.time)
    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
    return Pair(dayNum, dayName)
}
