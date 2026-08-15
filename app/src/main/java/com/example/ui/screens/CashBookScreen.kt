package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashTransaction
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionEditBottomSheet
import com.example.ui.components.TransactionViewBottomSheet
import com.example.ui.theme.LaborBackground
import com.example.ui.theme.LaborBlue
import com.example.ui.theme.LaborDivider
import com.example.ui.theme.LaborError
import com.example.ui.theme.LaborSuccess
import com.example.ui.theme.LaborTextHint
import com.example.ui.theme.LaborTextPrimary
import com.example.ui.theme.LaborTextSecondary
import com.example.ui.viewmodel.LaborViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.AppStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CashBookScreen(
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.filteredTransactions.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val searchQuery by viewModel.transactionSearchQuery.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val lang = userProfile.language

    val activeTransaction by viewModel.activeTransaction.collectAsState()
    val sheetMode by viewModel.transactionSheetMode.collectAsState()

    var showMonthDialog by remember { mutableStateOf(false) }

    // Filter transactions based on selected month if specific month selected
    val displayTransactions = remember(transactions, selectedMonth) {
        if (selectedMonth == "All Months" || selectedMonth.isBlank()) {
            transactions
        } else {
            // E.g. "Aug 2026" matches "2026-08" or "Aug"
            val monthMap = mapOf(
                "Jan" to "01", "Feb" to "02", "Mar" to "03", "Apr" to "04",
                "May" to "05", "Jun" to "06", "Jul" to "07", "Aug" to "08",
                "Sep" to "09", "Oct" to "10", "Nov" to "11", "Dec" to "12"
            )
            val monthPart = selectedMonth.take(3)
            val monthNum = monthMap[monthPart]
            transactions.filter { tx ->
                if (monthNum != null && tx.fullDate.contains("-$monthNum-")) true
                else tx.dateDisplay.contains(monthPart, ignoreCase = true) || tx.fullDate.contains(selectedMonth)
            }.ifEmpty {
                // If filtered list is empty but we have transactions, don't hide everything if month matching format differs
                transactions.filter { it.dateDisplay.contains(monthPart, ignoreCase = true) || it.notes.contains(monthPart, ignoreCase = true) }
            }.ifEmpty { transactions }
        }
    }

    val cashInTotal = displayTransactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
    val cashOutTotal = displayTransactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
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
            containerColor = LaborBackground,
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 2.dp
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

                        // Month Selector Pill (Calendar icon + Month + chevron) -> Functional Calendar Click
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF3F4F6),
                            border = BorderStroke(1.dp, LaborDivider),
                            modifier = Modifier
                                .clickable { showMonthDialog = true }
                                .testTag("cashbook_month_selector_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Select Calendar Month",
                                    tint = LaborBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedMonth,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LaborTextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = LaborTextSecondary,
                                    modifier = Modifier.size(16.dp)
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
                    shadowElevation = 8.dp
                ) {
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
                            colors = ButtonDefaults.buttonColors(containerColor = LaborSuccess)
                        ) {
                            Text(
                                text = AppStrings.get("cash_in", lang),
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
                            colors = ButtonDefaults.buttonColors(containerColor = LaborError)
                        ) {
                            Text(
                                text = AppStrings.get("cash_out", lang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onTransactionSearchQueryChanged(it) },
                        placeholder = { Text(AppStrings.get("search_transactions", lang), color = LaborTextHint, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_search_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LaborBlue,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                item { Spacer(modifier = Modifier.height(14.dp)) }

                // Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Row 1: Cash In
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.get("cash_in_label", lang),
                                    fontSize = 14.sp,
                                    color = LaborTextSecondary
                                )
                                Text(
                                    text = "₹ ${cashInTotal.toInt()}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborSuccess
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Row 2: Cash Out
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.get("cash_out_label", lang),
                                    fontSize = 14.sp,
                                    color = LaborTextSecondary
                                )
                                Text(
                                    text = "₹ ${cashOutTotal.toInt()}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborError
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = LaborDivider, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(12.dp))

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
                                    color = Color.Black
                                )
                                Text(
                                    text = "₹ ${balance.toInt()}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 'View Report' light blue pill button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .clickable { viewModel.navigateTo(Screen.CashBookReport) }
                                    .padding(vertical = 10.dp)
                                    .testTag("cashbook_view_report_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = LaborBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = AppStrings.get("view_report", lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LaborBlue
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Transaction List / Empty State
                if (displayTransactions.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = AppStrings.get("no_transactions", lang),
                                fontSize = 15.sp,
                                color = LaborTextSecondary
                            )
                        }
                    }
                } else {
                    // Header Row
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = AppStrings.get("date", lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = AppStrings.get("notes", lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.weight(2f)
                                    )
                                    Text(
                                        text = AppStrings.get("amount", lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.weight(1.4f)
                                    )
                                }
                                HorizontalDivider(color = LaborDivider, thickness = 0.5.dp)
                            }
                        }
                    }

                    items(displayTransactions, key = { it.id }) { tx ->
                        Surface(
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openTransactionDetail(tx) }
                                .testTag("tx_row_${tx.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Date Column
                                Column(
                                    modifier = Modifier.weight(1.2f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val parts = tx.dateDisplay.split(" ")
                                    val dayPart = parts.firstOrNull() ?: "15"
                                    val weekPart = parts.getOrNull(1) ?: "Sat"

                                    Text(
                                        text = dayPart,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LaborTextPrimary
                                    )
                                    Text(
                                        text = weekPart,
                                        fontSize = 11.sp,
                                        color = LaborTextSecondary
                                    )
                                }

                                // Notes & Payment Method Caption
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(
                                        text = tx.notes.ifBlank { if (tx.type == TransactionType.CASH_IN) "Income" else "Expense" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LaborTextPrimary
                                    )
                                    Text(
                                        text = tx.paymentMethod.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LaborTextSecondary
                                    )
                                }

                                // ₹ Amount + Chevron
                                Row(
                                    modifier = Modifier.weight(1.4f),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "₹ ${tx.amount.toInt()}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.type == TransactionType.CASH_IN) LaborSuccess else LaborError
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = LaborTextHint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = LaborDivider, thickness = 0.5.dp)
                    }

                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
            }
        }

        // Calendar Month Selection Dialog
        if (showMonthDialog) {
            AlertDialog(
                onDismissRequest = { showMonthDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = LaborBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Cash Book Month", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        items(monthsList) { m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateSelectedMonth(m)
                                        showMonthDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = m,
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedMonth == m) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedMonth == m) LaborBlue else LaborTextPrimary
                                )
                                if (selectedMonth == m) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFEFF6FF)
                                    ) {
                                        Text(
                                            text = "Active",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LaborBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF3F4F6))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showMonthDialog = false
                            datePickerDialog.show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                    ) {
                        Text("Pick Custom Month / Date", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMonthDialog = false }) {
                        Text(AppStrings.get("cancel", lang))
                    }
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
