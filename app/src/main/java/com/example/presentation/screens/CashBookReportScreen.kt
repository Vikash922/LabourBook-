package com.example.presentation.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.CashTransaction
import com.example.domain.model.TransactionType
import com.example.presentation.viewmodel.LaborViewModel
import com.example.presentation.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CashBookReportScreen(
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    val displayPillFormat = remember { SimpleDateFormat("EEE, dd MMM yy", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var startCalState by remember { mutableStateOf(Calendar.getInstance()) }
    var endCalState by remember { mutableStateOf(Calendar.getInstance()) }

    // Initialize date range based on selected month (e.g. "Jul 2026")
    LaunchedEffect(selectedMonth) {
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val parsedDate = try { monthFormat.parse(selectedMonth) } catch (e: Exception) { null }

        if (parsedDate != null) {
            cal.time = parsedDate
        }

        val start = Calendar.getInstance().apply {
            time = cal.time
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val end = Calendar.getInstance().apply {
            time = cal.time
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        startCalState = start
        endCalState = end
    }

    val startDatePillText = remember(startCalState) { displayPillFormat.format(startCalState.time) }
    val endDatePillText = remember(endCalState) { displayPillFormat.format(endCalState.time) }

    // Date Pickers
    val startDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCal = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
            }
            startCalState = newCal
        },
        startCalState.get(Calendar.YEAR),
        startCalState.get(Calendar.MONTH),
        startCalState.get(Calendar.DAY_OF_MONTH)
    )

    val endDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCal = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 23, 59, 59)
            }
            endCalState = newCal
        },
        endCalState.get(Calendar.YEAR),
        endCalState.get(Calendar.MONTH),
        endCalState.get(Calendar.DAY_OF_MONTH)
    )

    // Filter transactions within selected date range asynchronously on Dispatchers.Default
    var reportTransactions by remember { mutableStateOf<List<CashTransaction>>(emptyList()) }

    LaunchedEffect(transactions, startCalState, endCalState) {
        val startMillis = startCalState.timeInMillis
        val endMillis = endCalState.timeInMillis
        val currentTxList = transactions

        val filtered = withContext(Dispatchers.Default) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            currentTxList.filter { tx ->
                if (tx.amount <= 0.0) return@filter false
                val txMillis = if (tx.fullDate.isNotBlank()) {
                    try {
                        sdf.parse(tx.fullDate)?.time ?: tx.timestamp
                    } catch (e: Exception) {
                        tx.timestamp
                    }
                } else {
                    tx.timestamp
                }
                txMillis in startMillis..endMillis
            }.sortedWith(
                compareByDescending<CashTransaction> { it.fullDate.ifBlank { "0000-00-00" } }
                    .thenByDescending { it.timestamp }
            )
        }
        reportTransactions = filtered
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.CashBook) },
                        modifier = Modifier.testTag("cb_report_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cash Book Report",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Column {
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Share PDF Button
                        Button(
                            onClick = { viewModel.shareCashBookReport(startDatePillText, endDatePillText, reportTransactions) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("download_report_btn"),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D61E7))
                        ) {
                            Text(
                                text = "Share PDF",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Share WhatsApp Button
                        Button(
                            onClick = { viewModel.shareCashBookReport(startDatePillText, endDatePillText, reportTransactions) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("share_cb_report_btn"),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00875A))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share",
                                fontSize = 15.sp,
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
                .padding(innerPadding)
        ) {
            item {
                // Date Pills Container Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Date Pill
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { startDatePickerDialog.show() }
                            .testTag("report_start_date_pill"),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.2.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Start Date",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = startDatePillText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    // End Date Pill
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { endDatePickerDialog.show() }
                            .testTag("report_end_date_pill"),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.2.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "End Date",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = endDatePillText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            item {
                // Main Compact Grid Table matching reference screenshot
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
                        // Light Table Header Row with Vertical Dividers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFAFAFA))
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Date",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(0.20f)
                                    .padding(vertical = 10.dp)
                            )
                            VerticalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())

                            Text(
                                text = "Notes",
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
                                text = "₹ Amount",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827),
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .weight(0.36f)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)

                        // Table Data Rows
                        if (reportTransactions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions found in this period.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                reportTransactions.forEachIndexed { index, tx ->
                                    val (dayNum, dayName) = parseDateDetails(tx.fullDate, tx.dateDisplay, tx.timestamp)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Column 1: Date
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

                                        // Column 2: Notes
                                        Box(
                                            modifier = Modifier
                                                .weight(0.44f)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = tx.notes.ifBlank { "-" },
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color(0xFF111827),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        VerticalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())

                                        // Column 3: Amount (Left aligned in its column)
                                        Box(
                                            modifier = Modifier
                                                .weight(0.36f)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            val formattedAmount = if (tx.amount % 1.0 == 0.0) {
                                                "${tx.amount.toInt()}.0"
                                            } else {
                                                "${tx.amount}"
                                            }
                                            val isCashIn = tx.type == TransactionType.CASH_IN

                                            Text(
                                                text = "₹ $formattedAmount",
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCashIn) Color(0xFF16A34A) else Color(0xFFDC2626)
                                            )
                                        }
                                    }

                                    if (index < reportTransactions.size - 1) {
                                        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun parseDateDetails(fullDate: String, dateDisplay: String, timestamp: Long): Pair<String, String> {
    if (fullDate.isNotBlank()) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(fullDate)
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
        if (parts.size >= 2) {
            return Pair(parts[0], parts[1])
        }
    }
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val dayNum = SimpleDateFormat("d", Locale.getDefault()).format(cal.time)
    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
    return Pair(dayNum, dayName)
}

