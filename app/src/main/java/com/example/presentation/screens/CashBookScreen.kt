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
            // E.g. "Aug 2026" matches "2026-08" or "Aug"
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
                    tx.fullDate.startsWith("$yearPart-$monthNum") || tx.fullDate.contains("-$monthNum-")
                } else false

                val matchDisplayDate = tx.dateDisplay.contains(monthPart, ignoreCase = true) &&
                        (yearPart.isBlank() || tx.dateDisplay.contains(yearPart) || tx.fullDate.contains(yearPart))

                matchFullDate || matchDisplayDate
            }
        }
    }

    val cashInTotal = remember(displayTransactions) { displayTransactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount } }
    val cashOutTotal = remember(displayTransactions) { displayTransactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount } }
    val balance = cashInTotal - cashOutTotal

    val monthsList = listOf(
        "Aug 2026", "Jul 2026", "Jun 2026", "May 2026", "Apr 2026",
        "Sep 2026", "Oct 2026", "Nov 2026", "Dec 2026", "All Months"
    )

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
                                // Table Header Row with Vertical Dividers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF9FAFB))
                                        .height(IntrinsicSize.Min),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = AppStrings.get("date", lang),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827),
                                        modifier = Modifier
                                            .weight(0.22f)
                                            .padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    VerticalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())
                                    Text(
                                        text = AppStrings.get("notes", lang),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827),
                                        modifier = Modifier
                                            .weight(0.48f)
                                            .padding(horizontal = 8.dp, vertical = 8.dp)
                                    )
                                    VerticalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())
                                    Text(
                                        text = "₹ " + AppStrings.get("amount", lang),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827),
                                        modifier = Modifier
                                            .weight(0.30f)
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        textAlign = TextAlign.End
                                    )
                                }

                                HorizontalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp)

                                // Table Items with Vertical Grid Dividers
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
                                                .weight(0.22f)
                                                .padding(vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNum,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF111827)
                                            )
                                            Text(
                                                text = dayName,
                                                fontSize = 10.sp,
                                                color = Color(0xFF6B7280)
                                            )
                                        }

                                        VerticalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())

                                        // Notes & Payment Method Column
                                        Column(
                                            modifier = Modifier
                                                .weight(0.48f)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = tx.notes.ifBlank { "-" },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF111827),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = tx.paymentMethod.name.uppercase(),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6B7280)
                                            )
                                        }

                                        VerticalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())

                                        // Amount Column with Chevron Arrow
                                        Row(
                                            modifier = Modifier
                                                .weight(0.30f)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val formattedAmount = if (tx.amount % 1.0 == 0.0) {
                                                "${tx.amount.toInt()}"
                                            } else {
                                                "${tx.amount}"
                                            }
                                            Text(
                                                text = "₹$formattedAmount",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tx.type == TransactionType.CASH_IN) Color(0xFF16A34A) else Color(0xFFDC2626)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = Color(0xFF9CA3AF),
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
