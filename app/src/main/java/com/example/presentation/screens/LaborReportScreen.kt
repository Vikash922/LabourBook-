package com.example.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.util.LaborCalendarHelper
import com.example.core.util.PdfReportGenerator
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.LaborWorker
import com.example.presentation.theme.LaborBlue
import com.example.presentation.viewmodel.LaborViewModel
import com.example.presentation.viewmodel.Screen
import java.util.Locale

@Composable
fun LaborReportScreen(
    workerId: String,
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val workers by viewModel.workers.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val worker = remember(workers, workerId) { workers.firstOrNull { it.id == workerId } }

    if (worker == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Worker not found", color = Color(0xFF64748B))
        }
        return
    }

    // 1. Calculate Accurate Month Stats
    val (year, monthNum) = LaborCalendarHelper.parseYearMonth(selectedMonth)
    val fullMonthName = "${LaborCalendarHelper.monthsFull.getOrElse(monthNum - 1) { "August" }} $year"

    val monthAttendance = worker.getAttendanceForMonth(selectedMonth)

    var presentDaysCount = 0
    var absentDaysCount = 0
    var halfDayCount = 0.0
    var presentHalfCount = 0.0
    var doubleCount = 0.0
    var paidLeaveCount = 0.0
    var totalOvertimeHours = 0.0
    var totalAdvanceAmount = 0.0
    var totalOtEarnings = 0.0
    val defaultOtRatePerHour = if (worker.dailyWage > 0) (worker.dailyWage / 8.0) * 1.5 else 0.0

    for (rec in monthAttendance.values) {
        when (rec.status) {
            AttendanceStatus.PRESENT -> presentDaysCount++
            AttendanceStatus.ABSENT -> absentDaysCount++
            AttendanceStatus.HALF_DAY -> halfDayCount += 1.0
            AttendanceStatus.PRESENT_HALF -> presentHalfCount += 1.0
            AttendanceStatus.DOUBLE -> doubleCount += 1.0
            AttendanceStatus.PAID_LEAVE -> paidLeaveCount += 1.0
            AttendanceStatus.OVERTIME -> presentDaysCount++
            AttendanceStatus.UNMARKED -> {}
        }
        totalOvertimeHours += rec.overtimeHours
        totalAdvanceAmount += rec.advanceAmount
        val effectiveOtRate = if (rec.overtimeRate > 0.0) rec.overtimeRate else defaultOtRatePerHour
        totalOtEarnings += (rec.overtimeHours * effectiveOtRate)
    }

    val effectivePresentUnits = (presentDaysCount * 1.0) +
            (halfDayCount * 0.5) +
            (presentHalfCount * 1.5) +
            (doubleCount * 2.0) +
            (paidLeaveCount * 1.0)

    val totalEarnings = (effectivePresentUnits * worker.dailyWage) + totalOtEarnings
    val netBalance = totalEarnings - totalAdvanceAmount

    val slipText = PdfReportGenerator.generateWorkerReportText(worker, selectedMonth)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.LaborDetail(worker.id)) },
                        modifier = Modifier.testTag("report_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = "Worker Report",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Month pill indicator
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDBEAFE)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = LaborBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = fullMonthName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LaborBlue
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Primary 'Share PDF' Blue Pill Button
                    Button(
                        onClick = { viewModel.shareWorkerReport(worker) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("share_pdf_btn"),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1656D6))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share PDF",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Quick WhatsApp Share Button
                    Surface(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.shareWorkerReport(worker) },
                        shape = CircleShape,
                        color = Color(0xFF25D366)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = "Share on WhatsApp",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
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
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Worker Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFEFF6FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = worker.name.take(1).uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = worker.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            if (worker.phoneNumber.isNotBlank()) {
                                Text(
                                    text = worker.phoneNumber,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        // Daily Wage Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "Rate: ₹${worker.dailyWage.toInt()}/day",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Attendance Section Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "ATTENDANCE SUMMARY",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.6.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 1: Present | Absent | Overtime
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportMetricBox(
                                label = "Present",
                                value = "$presentDaysCount",
                                valueColor = Color(0xFF16A34A),
                                bgColor = Color(0xFFF0FDF4),
                                borderColor = Color(0xFFDCFCE7),
                                modifier = Modifier.weight(1f)
                            )
                            ReportMetricBox(
                                label = "Absent",
                                value = "$absentDaysCount",
                                valueColor = Color(0xFFDC2626),
                                bgColor = Color(0xFFFEF2F2),
                                borderColor = Color(0xFFFEE2E2),
                                modifier = Modifier.weight(1f)
                            )
                            val otDisplay = if (totalOvertimeHours % 1.0 == 0.0) "${totalOvertimeHours.toInt()}h" else "${String.format(Locale.ENGLISH, "%.1f", totalOvertimeHours)}h"
                            ReportMetricBox(
                                label = "Overtime",
                                value = otDisplay,
                                valueColor = Color(0xFF0F172A),
                                bgColor = Color(0xFFF8FAFC),
                                borderColor = Color(0xFFE2E8F0),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: Half Day | P + 1/2 | P+P
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val halfDayDisplay = if (halfDayCount % 1.0 == 0.0) "${halfDayCount.toInt()}" else String.format(Locale.ENGLISH, "%.1f", halfDayCount)
                            ReportMetricBox(
                                label = "Half Day",
                                value = halfDayDisplay,
                                valueColor = Color(0xFFD97706),
                                bgColor = Color(0xFFFFFBEB),
                                borderColor = Color(0xFFFEF3C7),
                                modifier = Modifier.weight(1f)
                            )
                            val presentHalfDisplay = String.format(Locale.ENGLISH, "%.1f", presentHalfCount)
                            ReportMetricBox(
                                label = "P + 1/2",
                                value = presentHalfDisplay,
                                valueColor = Color(0xFF0F172A),
                                bgColor = Color(0xFFF8FAFC),
                                borderColor = Color(0xFFE2E8F0),
                                modifier = Modifier.weight(1f)
                            )
                            val doubleDisplay = String.format(Locale.ENGLISH, "%.1f", doubleCount)
                            ReportMetricBox(
                                label = "P+P",
                                value = doubleDisplay,
                                valueColor = Color(0xFF0F172A),
                                bgColor = Color(0xFFF8FAFC),
                                borderColor = Color(0xFFE2E8F0),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Paid Leave (Optional Row if > 0)
                        if (paidLeaveCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReportMetricBox(
                                    label = "Paid Leave (PA)",
                                    value = "${paidLeaveCount.toInt()}",
                                    valueColor = Color(0xFF7C3AED),
                                    bgColor = Color(0xFFF5F3FF),
                                    borderColor = Color(0xFFEDE9FE),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.weight(2f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Payment Card (Compact & Professional)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "PAYMENT BREAKDOWN",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.6.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Advance Amount Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Advance Amount",
                                fontSize = 13.5.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = String.format(Locale.ENGLISH, "₹%,.2f", totalAdvanceAmount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Total Earnings Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Earnings",
                                fontSize = 13.5.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = String.format(Locale.ENGLISH, "₹%,.2f", totalEarnings),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Net Balance Highlight
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Net Balance Payable",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = String.format(Locale.ENGLISH, "₹%,.2f", netBalance),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netBalance >= 0) Color(0xFF1656D6) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Formatted Slip Preview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFFEFF6FF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = LaborBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Formatted Slip Preview",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = selectedMonth,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // Copy Slip Text Button
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEFF6FF),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDBEAFE)),
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                                    clipboardManager.setText(AnnotatedString(slipText))
                                    Toast.makeText(context, "Slip copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = LaborBlue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Copy",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LaborBlue
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Monospace Slip Container
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = slipText,
                                modifier = Modifier.padding(10.dp),
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF334155),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ReportMetricBox(
    label: String,
    value: String,
    valueColor: Color,
    bgColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}
