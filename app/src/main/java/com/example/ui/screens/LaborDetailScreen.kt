package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.LaborWorker
import com.example.ui.theme.LaborBackground
import com.example.ui.theme.LaborBlue
import com.example.ui.theme.LaborDivider
import com.example.ui.theme.LaborError
import com.example.ui.theme.LaborPurple
import com.example.ui.theme.LaborSuccess
import com.example.ui.theme.LaborTextPrimary
import com.example.ui.theme.LaborTextSecondary
import com.example.ui.viewmodel.LaborViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.LaborCalendarHelper

@Composable
fun LaborDetailScreen(
    workerId: String,
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val workers by viewModel.workers.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val worker = workers.firstOrNull { it.id == workerId }

    if (worker == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Worker not found", color = LaborTextSecondary)
        }
        return
    }

    val (currentYear, currentMonthNum) = LaborCalendarHelper.parseYearMonth(selectedMonth)
    val daysInCurrentMonth = LaborCalendarHelper.getDaysInMonth(currentYear, currentMonthNum)
    val context = LocalContext.current

    val monthPresent = worker.getTotalPresent(selectedMonth)
    val monthAbsent = worker.getTotalAbsent(selectedMonth)
    val monthOvertime = worker.getTotalOvertimeHours(selectedMonth)
    val monthAdvance = worker.getTotalAdvance(selectedMonth)
    val monthEstimatedEarnings = worker.getEstimatedEarnings(selectedMonth)

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditWorkerDialog by remember { mutableStateOf(false) }
    var selectedDayForAdvanceDialog by remember { mutableStateOf<Int?>(null) }
    var selectedDayForOvertimeDialog by remember { mutableStateOf<Int?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LaborBackground,
        topBar = {
            // App Bar: White background, back arrow, worker's name title, Edit button, Delete icon
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.navigateTo(Screen.LaborHome) },
                            modifier = Modifier.testTag("worker_detail_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = worker.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showEditWorkerDialog = true }
                                .testTag("worker_edit_pill_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                fontSize = 16.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = LaborError,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showDeleteConfirmDialog = true }
                                .testTag("worker_delete_icon_btn")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.shareWorkerReport(worker) },
                containerColor = Color.Black,
                contentColor = Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                },
                text = {
                    Text(
                        text = "Share to ${worker.name.split(" ").first()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Overview Card Header (Overview label left, month selector pill right)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Overview",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LaborTextSecondary
                    )

                    // Month Selector Pill
                    Row(
                        modifier = Modifier
                            .clickable { showCalendarDialog = true }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .testTag("month_selector_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
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
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }

            // Summary Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LaborDivider),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Daily rate", fontSize = 12.sp, color = LaborTextSecondary)
                                Text("₹${worker.dailyWage.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LaborTextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Estimated net wage", fontSize = 12.sp, color = LaborTextSecondary)
                                Text("₹${monthEstimatedEarnings.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LaborBlue)
                            }
                        }

                        HorizontalDivider(color = LaborDivider, thickness = 0.8.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Total Present
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (monthPresent % 1.0 == 0.0) monthPresent.toInt().toString() else monthPresent.toString(),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborSuccess
                                )
                                Text(
                                    text = "Total Present",
                                    fontSize = 11.sp,
                                    color = LaborTextSecondary
                                )
                            }

                            // 2. Total Absent
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = monthAbsent.toInt().toString(),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborError
                                )
                                Text(
                                    text = "Total Absent",
                                    fontSize = 11.sp,
                                    color = LaborTextSecondary
                                )
                            }

                            // 3. Over time
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${monthOvertime.toInt()}h",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborTextPrimary
                                )
                                Text(
                                    text = "Over time",
                                    fontSize = 11.sp,
                                    color = LaborTextSecondary
                                )
                            }

                            // 4. Total Advance
                            Column(
                                modifier = Modifier.weight(1.1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "₹${monthAdvance.toInt()}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborTextPrimary
                                )
                                Text(
                                    text = "Total Advance",
                                    fontSize = 11.sp,
                                    color = LaborTextSecondary
                                )
                            }
                        }

                        // Open Report Link
                        HorizontalDivider(color = LaborDivider, thickness = 0.8.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEFF6FF))
                                .clickable {
                                    viewModel.navigateTo(Screen.LaborReport(worker.id))
                                }
                                .padding(vertical = 12.dp)
                                .testTag("open_worker_report_btn"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = LaborBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open Report",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = LaborBlue
                            )
                        }
                        HorizontalDivider(color = LaborDivider, thickness = 0.8.dp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            // Daily Attendance Table
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 70.dp)
                ) {
                    // Column Headers: 'Date' | 'Attendance' | '₹ / Notes'
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FAFB))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaborTextSecondary,
                            modifier = Modifier.weight(1.1f)
                        )
                        Text(
                            text = "Attendance",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaborTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(2.3f)
                        )
                        Text(
                            text = "₹ / Notes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaborTextSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.3f)
                        )
                    }

                    HorizontalDivider(color = LaborDivider, thickness = 0.8.dp)

                    // Month days sequential list
                    for (day in 1..daysInCurrentMonth) {
                        val dateKey = LaborCalendarHelper.getDateKey(currentYear, currentMonthNum, day)
                        val dow = LaborCalendarHelper.getDayOfWeekShort(currentYear, currentMonthNum, day)
                        val isToday = LaborCalendarHelper.isToday(currentYear, currentMonthNum, day)

                        val dayRecord = worker.attendance[dateKey]
                        val status = dayRecord?.status ?: AttendanceStatus.UNMARKED
                        val advance = dayRecord?.advanceAmount ?: 0.0
                        val note = dayRecord?.note ?: ""
                        val otHours = dayRecord?.overtimeHours ?: 0.0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isToday) Color(0xFFEFF6FF) else Color.White)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Date & Day of Week
                            Column(modifier = Modifier.weight(1.1f)) {
                                Text(
                                    text = String.format("%02d %s", day, dow),
                                    fontSize = 14.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isToday) LaborBlue else LaborTextPrimary
                                )
                                if (isToday) {
                                    Text(
                                        text = "Today",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LaborBlue
                                    )
                                }
                            }

                            // Column 2: Status Chips [P] [A] [OT] [HD]
                            Row(
                                modifier = Modifier.weight(2.3f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // P - Present (Green)
                                DetailStatusChip(
                                    label = "P",
                                    color = LaborSuccess,
                                    isSelected = status == AttendanceStatus.PRESENT,
                                    onClick = {
                                        viewModel.setAttendance(worker.id, day, AttendanceStatus.PRESENT, selectedMonth)
                                    }
                                )

                                // A - Absent (Red)
                                DetailStatusChip(
                                    label = "A",
                                    color = LaborError,
                                    isSelected = status == AttendanceStatus.ABSENT,
                                    onClick = {
                                        viewModel.setAttendance(worker.id, day, AttendanceStatus.ABSENT, selectedMonth)
                                    }
                                )

                                // OT - Overtime (Blue)
                                DetailStatusChip(
                                    label = if (status == AttendanceStatus.OVERTIME && otHours > 0) "${otHours.toInt()}h" else "OT",
                                    color = LaborBlue,
                                    isSelected = status == AttendanceStatus.OVERTIME,
                                    onClick = {
                                        selectedDayForOvertimeDialog = day
                                    }
                                )

                                // HD - Half Day (Purple)
                                DetailStatusChip(
                                    label = "HD",
                                    color = LaborPurple,
                                    isSelected = status == AttendanceStatus.HALF_DAY,
                                    onClick = {
                                        viewModel.setAttendance(worker.id, day, AttendanceStatus.HALF_DAY, selectedMonth)
                                    }
                                )
                            }

                            // Column 3: Notes / Advance Pill
                            Column(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .clickable { selectedDayForAdvanceDialog = day },
                                horizontalAlignment = Alignment.End
                            ) {
                                if (advance > 0) {
                                    Text(
                                        text = "₹${advance.toInt()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LaborError
                                    )
                                }
                                if (note.isNotBlank()) {
                                    Text(
                                        text = note,
                                        fontSize = 11.sp,
                                        color = LaborTextSecondary,
                                        maxLines = 1
                                    )
                                }
                                if (advance == 0.0 && note.isBlank()) {
                                    Text(
                                        text = "+ Add",
                                        fontSize = 12.sp,
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = LaborDivider.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    // Advance & Notes Dialog for specific day
    selectedDayForAdvanceDialog?.let { day ->
        val dateKey = LaborCalendarHelper.getDateKey(currentYear, currentMonthNum, day)
        val dayRecord = worker.attendance[dateKey]
        val dow = LaborCalendarHelper.getDayOfWeekShort(currentYear, currentMonthNum, day)
        var advanceInput by remember { mutableStateOf(if ((dayRecord?.advanceAmount ?: 0.0) > 0) (dayRecord?.advanceAmount ?: 0.0).toInt().toString() else "") }
        var noteInput by remember { mutableStateOf(dayRecord?.note ?: "") }

        AlertDialog(
            onDismissRequest = { selectedDayForAdvanceDialog = null },
            title = {
                Text(
                    text = "Advance & Notes for $day $dow ($selectedMonth)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = advanceInput,
                        onValueChange = { advanceInput = it },
                        label = { Text("Advance Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val adv = advanceInput.toDoubleOrNull() ?: 0.0
                        viewModel.updateDayDetails(
                            workerId = worker.id,
                            dayNumber = day,
                            advance = adv,
                            note = noteInput,
                            otHours = dayRecord?.overtimeHours ?: 0.0,
                            monthStr = selectedMonth
                        )
                        selectedDayForAdvanceDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDayForAdvanceDialog = null }) {
                    Text("Cancel", color = LaborTextSecondary)
                }
            }
        )
    }

    // Overtime Dialog for specific day
    selectedDayForOvertimeDialog?.let { day ->
        val dateKey = LaborCalendarHelper.getDateKey(currentYear, currentMonthNum, day)
        val dayRecord = worker.attendance[dateKey]
        val dow = LaborCalendarHelper.getDayOfWeekShort(currentYear, currentMonthNum, day)
        var otHoursInput by remember { mutableStateOf(if ((dayRecord?.overtimeHours ?: 0.0) > 0) (dayRecord?.overtimeHours ?: 0.0).toInt().toString() else "2") }

        AlertDialog(
            onDismissRequest = { selectedDayForOvertimeDialog = null },
            title = {
                Text(
                    text = "Log Overtime: $day $dow ($selectedMonth)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    Text("Enter extra overtime hours worked by ${worker.name}:", fontSize = 13.sp, color = LaborTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otHoursInput,
                        onValueChange = { otHoursInput = it },
                        label = { Text("Overtime Hours (e.g. 2, 4)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ot = otHoursInput.toDoubleOrNull() ?: 2.0
                        viewModel.updateDayDetails(
                            workerId = worker.id,
                            dayNumber = day,
                            advance = dayRecord?.advanceAmount ?: 0.0,
                            note = dayRecord?.note ?: "",
                            otHours = ot,
                            monthStr = selectedMonth
                        )
                        viewModel.setAttendance(worker.id, day, AttendanceStatus.OVERTIME, selectedMonth)
                        selectedDayForOvertimeDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                ) {
                    Text("Save Overtime", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDayForOvertimeDialog = null }) {
                    Text("Cancel", color = LaborTextSecondary)
                }
            }
        )
    }

    // Month Selector Dialog
    if (showCalendarDialog) {
        val months = LaborCalendarHelper.monthsShort
        val years = LaborCalendarHelper.years
        var selectedTempMonth by remember { mutableStateOf(selectedMonth.split(" ").firstOrNull() ?: "Aug") }
        var selectedTempYear by remember { mutableStateOf(selectedMonth.split(" ").getOrNull(1) ?: "2026") }
        var monthExpanded by remember { mutableStateOf(false) }
        var yearExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCalendarDialog = false },
            title = { Text("Select Attendance Month", fontWeight = FontWeight.Bold) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedTempMonth,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Month") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { monthExpanded = true }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            months.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month) },
                                    onClick = {
                                        selectedTempMonth = month
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedTempYear,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Year") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { yearExpanded = true }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false }
                        ) {
                            years.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year) },
                                    onClick = {
                                        selectedTempYear = year
                                        yearExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSelectedMonth("$selectedTempMonth $selectedTempYear")
                        showCalendarDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                ) {
                    Text("OK", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarDialog = false }) {
                    Text("Cancel", color = LaborTextSecondary)
                }
            }
        )
    }

    // Delete Worker Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Labor Record?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${worker.name}? This will permanently remove their attendance and wage logs.") },
            confirmButton = {
                Button(
                    onClick = {
                        val deletedName = worker.name
                        viewModel.deleteWorker(worker.id)
                        showDeleteConfirmDialog = false
                        viewModel.navigateTo(Screen.LaborHome)
                        android.widget.Toast.makeText(
                            context,
                            "$deletedName removed. Safety backup saved (Restore anytime from Settings > Drive Backup)",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LaborError)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Worker Dialog
    if (showEditWorkerDialog) {
        var editName by remember { mutableStateOf(worker.name) }
        var editPhone by remember { mutableStateOf(worker.phoneNumber) }
        var editWage by remember { mutableStateOf(worker.dailyWage.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showEditWorkerDialog = false },
            title = { Text("Edit Worker", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editWage,
                        onValueChange = { editWage = it },
                        label = { Text("Daily Wage (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newWage = editWage.toDoubleOrNull() ?: worker.dailyWage
                        viewModel.updateWorker(worker.id, editName, editPhone, newWage)
                        showEditWorkerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditWorkerDialog = false }) {
                    Text("Cancel", color = LaborTextSecondary)
                }
            }
        )
    }
}

@Composable
fun DetailStatusChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = 1.2.dp,
                color = color,
                shape = RoundedCornerShape(6.dp)
            )
            .background(if (isSelected) color else Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else color
        )
    }
}
