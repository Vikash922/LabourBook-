package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.DailyAttendance
import com.example.domain.model.LaborWorker
import com.example.presentation.theme.LaborBackground
import com.example.presentation.theme.LaborBlue
import com.example.presentation.theme.LaborDivider
import com.example.presentation.theme.LaborError
import com.example.presentation.theme.LaborPurple
import com.example.presentation.theme.LaborSuccess
import com.example.presentation.theme.LaborTextPrimary
import com.example.presentation.theme.LaborTextSecondary
import com.example.presentation.viewmodel.LaborViewModel
import com.example.presentation.viewmodel.Screen
import com.example.core.util.LaborCalendarHelper
import com.example.core.util.MonthDayInfo

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
    var selectedDayForAttendanceSheet by remember { mutableStateOf<Pair<Int, com.example.domain.model.AttendanceStatus?>?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var isOverviewExpanded by remember { mutableStateOf(false) }
    var isRefreshingOverview by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRefreshingOverview) 360f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        finishedListener = { isRefreshingOverview = false },
        label = "refresh_rotation"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            // App Bar: White background, back arrow, worker's name title, Edit button with pencil, Red Delete icon
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.navigateTo(Screen.LaborHome) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("worker_detail_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = worker.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        // Edit Pill Button with border
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFD1D5DB),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .clickable { showEditWorkerDialog = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("worker_edit_pill_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                fontSize = 13.sp,
                                color = Color(0xFF111827),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Red Delete Icon
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("worker_delete_icon_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Black Pill "Share to Akash" Button as in screenshot
            Surface(
                onClick = { viewModel.shareWorkerReport(worker) },
                color = Color.Black,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .padding(bottom = 8.dp, end = 4.dp)
                    .testTag("share_worker_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chat speech bubble icon
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF25D366), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share to ${worker.name.split(" ").first()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            // Overview Header (Overview label left, Month/Year selector pill right)
            item {
                Column {
                    HorizontalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FAFB))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Overview",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF)
                        )

                        // Month/Year Selector Pill: [ 📅 Aug 2026 ⌵ ]
                        Row(
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFD1D5DB),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .clickable { showCalendarDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("month_selector_pill"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedMonth,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp)
                }
            }

            // Summary Stats KPI Row (Total Present | Total Absent | Over time | Total Advance ⌵)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Total Present (Green)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (monthPresent % 1.0 == 0.0) "${monthPresent.toInt()}.0" else "$monthPresent",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Total Present",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                    }

                    // 2. Total Absent (Red)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (monthAbsent % 1.0 == 0.0) "${monthAbsent.toInt()}.0" else "$monthAbsent",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Total Absent",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                    }

                    // 3. Over time (Dark)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${monthOvertime.toInt()}h",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Over time",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                    }

                    // 4. Total Advance (Dark)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "₹${if (monthAdvance % 1.0 == 0.0) "${monthAdvance.toInt()}.0" else "$monthAdvance"}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Total Advance",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                    }

                    // 5. Chevron Arrow (Right edge)
                    IconButton(onClick = { isOverviewExpanded = !isOverviewExpanded }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp).rotate(if (isOverviewExpanded) 180f else 0f)
                        )
                    }
                }
            }

            // Open Report Banner (Light blue background, document icon, blue text)
            item {
                HorizontalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFF6FF)) // Clear light blue
                        .clickable { viewModel.navigateTo(Screen.LaborReport(worker.id)) }
                        .padding(vertical = 12.dp)
                        .testTag("open_worker_report_btn"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Report",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                }
                HorizontalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp)
            }

            // Table Header: Date | Attendance | ₹ / Notes with vertical grid lines
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = Color.Black, thickness = 2.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(Color.White)
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left outer border
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                    
                    // Col 1: Date
                    Box(
                        modifier = Modifier
                            .width(55.dp)
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Date",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                    }
                    // Vertical divider 1
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                    // Col 2: Attendance
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Attendance",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                    }
                    // Vertical divider 2
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                    // Col 3: ₹ / Notes
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "₹/Notes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                    }
                    
                    // Right outer border
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = Color.Black, thickness = 2.dp)
            }

            // Table Row items for all days of the month
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
                    onStatusSelected = { day, newStatus ->
                        viewModel.setAttendance(worker.id, day, newStatus, selectedMonth)
                    },
                    onOvertimeClicked = { day ->
                        selectedDayForOvertimeDialog = day
                    },
                    onAdvanceClicked = { day ->
                        selectedDayForAdvanceDialog = day
                    },
                    onOpenAttendanceSheet = { day, initialStatus ->
                        selectedDayForAttendanceSheet = Pair(day, initialStatus)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
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

    @OptIn(ExperimentalMaterial3Api::class)
    // Mark Attendance Bottom Sheet (Exact Match to Reference PDF & Image)
    selectedDayForAttendanceSheet?.let { (day, initialStatus) ->
        val dateKey = LaborCalendarHelper.getDateKey(currentYear, currentMonthNum, day)
        val dayRecord = worker.attendance[dateKey]
        var currentStatus by remember(day, dayRecord, initialStatus) { 
            mutableStateOf(initialStatus ?: dayRecord?.status ?: AttendanceStatus.UNMARKED) 
        }
        val (year, monthNum) = remember(selectedMonth) { LaborCalendarHelper.parseYearMonth(selectedMonth) }
        val monthShortName = when (monthNum) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> "Aug"
        }
        val dayFormatted = if (day < 10) "0$day" else "$day"
        val dateDisplayString = "$monthShortName $dayFormatted, $year"

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { selectedDayForAttendanceSheet = null },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                        .padding(top = 22.dp)
                ) {
                    // Header: Worker Name & Date
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = worker.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Mark Attendance",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF6B7280)
                            )
                        }

                        Text(
                            text = dateDisplayString,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            modifier = Modifier.padding(end = 40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Attendance Status Pills (Row 1 & Row 2)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Row 1: A, 1/2, P, PA
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AttendanceSheetPill(
                                modifier = Modifier.weight(1f),
                                label = "A",
                                isSelected = currentStatus == AttendanceStatus.ABSENT,
                                onClick = { currentStatus = AttendanceStatus.ABSENT }
                            )
                            AttendanceSheetPill(
                                modifier = Modifier.weight(1f),
                                label = "1/2",
                                isSelected = currentStatus == AttendanceStatus.HALF_DAY,
                                onClick = { currentStatus = AttendanceStatus.HALF_DAY }
                            )
                            AttendanceSheetPill(
                                modifier = Modifier.weight(1f),
                                label = "P",
                                isSelected = currentStatus == AttendanceStatus.PRESENT,
                                onClick = { currentStatus = AttendanceStatus.PRESENT }
                            )
                            AttendanceSheetPill(
                                modifier = Modifier.weight(1f),
                                label = "PA",
                                isSelected = currentStatus == AttendanceStatus.PAID_LEAVE,
                                onClick = { currentStatus = AttendanceStatus.PAID_LEAVE }
                            )
                        }

                        // Row 2: P + 1/2, P + P, OT
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AttendanceSheetPill(
                                modifier = Modifier.weight(1f),
                                label = "P + 1/2",
                                isSelected = currentStatus == AttendanceStatus.PRESENT_HALF,
                                onClick = { currentStatus = AttendanceStatus.PRESENT_HALF }
                            )
                            AttendanceSheetPill(
                                modifier = Modifier.weight(1f),
                                label = "P + P",
                                isSelected = currentStatus == AttendanceStatus.DOUBLE,
                                onClick = { currentStatus = AttendanceStatus.DOUBLE }
                            )

                            // OT Pill
                            val isOt = (dayRecord?.overtimeHours ?: 0.0) > 0 || currentStatus == AttendanceStatus.OVERTIME
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isOt) Color(0xFF8B5CF6) else Color(0xFFE5E7EB),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(if (isOt) Color(0xFF8B5CF6) else Color.White)
                                    .clickable {
                                        selectedDayForAttendanceSheet = null
                                        selectedDayForOvertimeDialog = day
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if ((dayRecord?.overtimeHours ?: 0.0) > 0) "${if (dayRecord!!.overtimeHours % 1.0 == 0.0) dayRecord.overtimeHours.toInt() else dayRecord.overtimeHours}h" else "OT",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOt) Color.White else Color(0xFF8B5CF6)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // MEANING Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .background(Color(0xFFEEF2FF), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE0E7FF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "MEANING:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MeaningItem(boldKey = "A", desc = " - Absent")
                            MeaningItem(boldKey = "1/2", desc = " - Half day")
                            MeaningItem(boldKey = "P", desc = " - Present")
                            MeaningItem(boldKey = "OT", desc = " - Overtime")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            MeaningItem(boldKey = "P + 1/2", desc = " - 1.5 day")
                            Spacer(modifier = Modifier.width(18.dp))
                            MeaningItem(boldKey = "P+P", desc = " - Double")
                            Spacer(modifier = Modifier.width(18.dp))
                            MeaningItem(boldKey = "PA", desc = " - Paid Leave")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom Action Buttons: Remove Marked and Ok
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Remove Marked Button (Outlined)
                        OutlinedButton(
                            onClick = {
                                viewModel.setAttendance(worker.id, day, AttendanceStatus.UNMARKED, selectedMonth)
                                selectedDayForAttendanceSheet = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF1D4ED8)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1D4ED8)
                            )
                        ) {
                            Text(
                                text = "Remove Marked",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D4ED8)
                            )
                        }

                        // Ok Button (Solid Filled Blue)
                        Button(
                            onClick = {
                                viewModel.setAttendance(worker.id, day, currentStatus, selectedMonth)
                                selectedDayForAttendanceSheet = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1D4ED8),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Ok",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Floating Close Button (top right, above the sheet background)
                IconButton(
                    onClick = { selectedDayForAttendanceSheet = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp)
                        .size(40.dp)
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF374151),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private val ChipCornerShape = RoundedCornerShape(6.dp)
private val RowTodayBg = Color(0xFFEFF6FF)
private val RowNormalBg = Color.White
private val DividerStrokeColor = Color(0xFFD1D5DB)

@Composable
fun LaborAttendanceDayRow(
    dayInfo: MonthDayInfo,
    status: AttendanceStatus,
    advance: Double,
    note: String,
    otHours: Double,
    onStatusSelected: (Int, AttendanceStatus) -> Unit,
    onOvertimeClicked: (Int) -> Unit,
    onAdvanceClicked: (Int) -> Unit,
    onOpenAttendanceSheet: (Int, com.example.domain.model.AttendanceStatus?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left outer border
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
            // Col 1: Date
            Column(
                modifier = Modifier
                    .width(55.dp)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val dayNumStr = if (dayInfo.day < 10) "0${dayInfo.day}" else "${dayInfo.day}"
                Text(
                    text = dayNumStr,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = dayInfo.dow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black
                )
            }
            // Vertical divider 1
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
            
            // Col 2: Attendance
            Row(
                modifier = Modifier
                    .weight(1.3f)
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // A
                    val isA = status == AttendanceStatus.ABSENT || status == AttendanceStatus.HALF_DAY || status == AttendanceStatus.PRESENT_HALF
                    AttendancePillButton(
                        label = "A",
                        isSelected = status == AttendanceStatus.ABSENT,
                        activeColor = Color(0xFFEF4444),
                        onClick = { onOpenAttendanceSheet(dayInfo.day, AttendanceStatus.ABSENT) }
                    )
                    // P
                    val isP = status == AttendanceStatus.PRESENT || status == AttendanceStatus.PRESENT_HALF || status == AttendanceStatus.DOUBLE
                    AttendancePillButton(
                        label = "P",
                        isSelected = isP,
                        activeColor = Color(0xFF10B981),
                        onClick = { onOpenAttendanceSheet(dayInfo.day, AttendanceStatus.PRESENT) }
                    )
                    // OT
                    val isOtActive = otHours > 0 || status == AttendanceStatus.OVERTIME
                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .widthIn(min = 24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                width = 1.dp,
                                color = Color(0xFF8B5CF6),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .background(if (isOtActive) Color(0xFF8B5CF6) else Color.White)
                            .clickable {
                                if (isOtActive) onOpenAttendanceSheet(dayInfo.day, null)
                                else onOvertimeClicked(dayInfo.day)
                            }
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (otHours > 0) "${if (otHours % 1.0 == 0.0) otHours.toInt() else otHours}h" else "OT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOtActive) Color.White else Color.Gray
                        )
                    }
                }

                // 3 dots
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Vertical divider 2
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
            
            // Col 3: ₹ / Notes
            Row(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .clickable { onAdvanceClicked(dayInfo.day) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Showing ₹ symbol
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (advance > 0) "₹${if (advance % 1.0 == 0.0) advance.toInt() else advance}" else "₹",
                        fontSize = 16.sp,
                        fontWeight = if (advance > 0) FontWeight.Bold else FontWeight.Normal,
                        color = Color.Black
                    )
                    if (note.isNotBlank()) {
                        Text(
                            text = note,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }
                
                // > icon
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Edit Note",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            // Right outer border
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
        }
        HorizontalDivider(color = Color.Black, thickness = 2.dp)
    }
}

@Composable
fun AttendanceSheetPill(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.5.dp,
                color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(8.dp)
            )
            .background(if (isSelected) Color(0xFF2563EB) else Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else Color(0xFF374151)
        )
    }
}

@Composable
fun MeaningItem(
    boldKey: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = boldKey,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = Color(0xFF111827)
        )
        Text(
            text = desc,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Color(0xFF4B5563)
        )
    }
}

@Composable
fun AttendancePillButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                color = activeColor,
                shape = RoundedCornerShape(4.dp)
            )
            .background(if (isSelected) activeColor else Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else activeColor
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
