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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow

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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.DailyAttendance
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
import com.example.util.MonthDayInfo

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

    val (currentYear, currentMonthNum) = remember(selectedMonth) { LaborCalendarHelper.parseYearMonth(selectedMonth) }
    val monthDaysInfo = remember(currentYear, currentMonthNum) { LaborCalendarHelper.getMonthDaysInfo(currentYear, currentMonthNum) }
    val context = LocalContext.current

    val monthStats = remember(worker, selectedMonth) { worker.calculateMonthStats(selectedMonth) }
    val monthPresent = monthStats.presentCount
    val monthAbsent = monthStats.absentCount
    val monthOvertime = monthStats.overtimeHours
    val monthAdvance = monthStats.totalAdvance
    val monthEstimatedEarnings = monthStats.estimatedEarnings

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

            // Daily Attendance Table Header
            item {
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
            }

            // Month days sequential list as individual items for zero-delay granular recomposition
            items(
                items = monthDaysInfo,
                key = { it.dateKey }
            ) { dayInfo ->
                val dayRecord = worker.attendance[dayInfo.dateKey]

                LaborAttendanceDayRow(
                    dayInfo = dayInfo,
                    status = dayRecord?.status ?: AttendanceStatus.UNMARKED,
                    advance = dayRecord?.advanceAmount ?: 0.0,
                    note = dayRecord?.note ?: "",
                    otHours = dayRecord?.overtimeHours ?: 0.0,
                    onStatusSelected = { day, status ->
                        viewModel.setAttendance(worker.id, day, status, selectedMonth)
                    },
                    onOvertimeClicked = { day ->
                        selectedDayForOvertimeDialog = day
                    },
                    onAdvanceClicked = { day ->
                        selectedDayForAdvanceDialog = day
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
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
        var otHoursInput by remember { mutableStateOf(if ((dayRecord?.overtimeHours ?: 0.0) > 0) (if (dayRecord!!.overtimeHours % 1.0 == 0.0) dayRecord.overtimeHours.toInt().toString() else dayRecord.overtimeHours.toString()) else "2") }

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if ((dayRecord?.overtimeHours ?: 0.0) > 0) {
                        TextButton(
                            onClick = {
                                viewModel.updateDayDetails(
                                    workerId = worker.id,
                                    dayNumber = day,
                                    advance = dayRecord?.advanceAmount ?: 0.0,
                                    note = dayRecord?.note ?: "",
                                    otHours = 0.0,
                                    monthStr = selectedMonth
                                )
                                selectedDayForOvertimeDialog = null
                            }
                        ) {
                            Text("Clear OT", color = LaborError)
                        }
                    }
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
                            selectedDayForOvertimeDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                    ) {
                        Text("Save Overtime", color = Color.White)
                    }
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
    // Month Selector Dialog
    if (showCalendarDialog) {
        MonthYearSelectionDialog(
            initialSelection = selectedMonth,
            onDismiss = { showCalendarDialog = false },
            onConfirm = { newSelection -> 
                viewModel.updateSelectedMonth(newSelection)
                showCalendarDialog = false 
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    // Delete Worker Confirmation Dialog
    if (showDeleteConfirmDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showDeleteConfirmDialog = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Floating Close Button (top right)
                IconButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp, top = 0.dp)
                        .size(40.dp)
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Black
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
                ) {
                    // Initial Letter
                    Text(
                        text = worker.name.take(1).uppercase(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Worker Name
                    Text(
                        text = worker.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Swipe to Delete Component
                    SwipeToConfirm(
                        onConfirm = {
                            viewModel.deleteWorker(worker.id)
                            showDeleteConfirmDialog = false
                            viewModel.navigateTo(Screen.LaborHome)
                        }
                    )
                }
            }
        }
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

private val ChipCornerShape = RoundedCornerShape(6.dp)
private val RowTodayBg = Color(0xFFEFF6FF)
private val RowNormalBg = Color.White
private val DividerStrokeColor = Color(0xFFE5E7EB)

@Composable
fun LaborAttendanceDayRow(
    dayInfo: MonthDayInfo,
    status: AttendanceStatus,
    advance: Double,
    note: String,
    otHours: Double,
    onStatusSelected: (Int, AttendanceStatus) -> Unit,
    onOvertimeClicked: (Int) -> Unit,
    onAdvanceClicked: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = DividerStrokeColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (dayInfo.isToday) RowTodayBg else RowNormalBg)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Column 1: Date & Day of Week
            Column(modifier = Modifier.weight(1.1f)) {
                Text(
                    text = dayInfo.formattedDisplay,
                    fontSize = 14.sp,
                    fontWeight = if (dayInfo.isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (dayInfo.isToday) LaborBlue else LaborTextPrimary
                )
                if (dayInfo.isToday) {
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
                    onClick = { onStatusSelected(dayInfo.day, AttendanceStatus.PRESENT) }
                )

                // A - Absent (Red)
                DetailStatusChip(
                    label = "A",
                    color = LaborError,
                    isSelected = status == AttendanceStatus.ABSENT,
                    onClick = { onStatusSelected(dayInfo.day, AttendanceStatus.ABSENT) }
                )

                // OT - Overtime (Blue)
                DetailStatusChip(
                    label = if (otHours > 0) "${if (otHours % 1.0 == 0.0) otHours.toInt() else otHours}h" else "OT",
                    color = LaborBlue,
                    isSelected = status == AttendanceStatus.OVERTIME || otHours > 0,
                    onClick = { onOvertimeClicked(dayInfo.day) }
                )

                // HD - Half Day (Purple)
                DetailStatusChip(
                    label = "HD",
                    color = LaborPurple,
                    isSelected = status == AttendanceStatus.HALF_DAY,
                    onClick = { onStatusSelected(dayInfo.day, AttendanceStatus.HALF_DAY) }
                )
            }

            // Column 3: Notes / Advance Pill
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .clickable { onAdvanceClicked(dayInfo.day) },
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
            .clip(ChipCornerShape)
            .border(
                width = 1.2.dp,
                color = color,
                shape = ChipCornerShape
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


@Composable
fun SwipeToConfirm(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var isConfirmed by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFB91C1C)) // Dark red background
    ) {
        // We subtract the thumb size (56dp) to get the max swipe distance
        val maxSwipePx = with(density) { (maxWidth - 56.dp).toPx() }
        
        // Background text
        Text(
            text = "Swipe to Delete Labor",
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Thumb (circular button)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                .padding(4.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF7F1D1D)) // Darker red thumb
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeOffset > maxSwipePx * 0.8f) {
                                swipeOffset = maxSwipePx
                                if (!isConfirmed) {
                                    isConfirmed = true
                                    onConfirm()
                                }
                            } else {
                                swipeOffset = 0f
                            }
                        }
                    ) { change, dragAmount ->
                        if (!isConfirmed) {
                            swipeOffset = (swipeOffset + dragAmount).coerceIn(0f, maxSwipePx)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Swipe to delete",
                tint = Color.White
            )
        }
    }
}
