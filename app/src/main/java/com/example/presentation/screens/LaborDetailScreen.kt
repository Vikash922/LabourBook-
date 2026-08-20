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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import com.example.presentation.components.MonthYearSelectionBottomSheet
import com.example.presentation.components.AdvanceAmountBottomSheet
import com.example.presentation.components.EditWorkerProfileBottomSheet
import com.example.domain.model.PaymentMethod
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.mutableIntStateOf

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

@OptIn(ExperimentalMaterial3Api::class)
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
                                imageVector = Icons.Default.ArrowBack,
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
                .background(Color(0xFFF8FAFC))
        ) {
            // Overview Header (Overview label left, Month/Year selector pill right)
            item {
                Column {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Overview",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )

                            // Month/Year Selector Pill: [ 📅 Aug 2026 ⌵ ] with subtle shadow
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showCalendarDialog = true }
                                    .testTag("month_selector_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFF1D61D2),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = selectedMonth,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Summary Stats KPI Row (Total Present | Total Absent | Over time | Total Advance ⌵)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
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
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
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
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
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
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Over time",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }

                        // 4. Total Advance (Red when paid, otherwise dark)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "₹${if (monthAdvance % 1.0 == 0.0) "${monthAdvance.toInt()}.0" else "$monthAdvance"}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (monthAdvance > 0) Color(0xFFDC2626) else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Total Advance",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }

                        // 5. Chevron Arrow (Right edge)
                        IconButton(
                            onClick = { isOverviewExpanded = !isOverviewExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = Color(0xFF1D61D2),
                                modifier = Modifier.size(24.dp).rotate(if (isOverviewExpanded) 180f else 0f)
                            )
                        }
                    }
                }
            }

            // Open Report Banner (Light blue background with shadow & rounded pill card)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.LaborReport(worker.id)) }
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF1D61D2),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Report",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D61D2)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Table Header: Date | Attendance | ₹ / Notes with clean slate background & sharp contrast
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Col 1: Date
                        Box(
                            modifier = Modifier
                                .width(68.dp)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Date",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        // Vertical divider 1
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFCBD5E1)))
                        
                        // Col 2: Attendance
                        Box(
                            modifier = Modifier
                                .weight(1.35f)
                                .fillMaxHeight()
                                .padding(start = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Attendance",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        // Vertical divider 2
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFCBD5E1)))
                        
                        // Col 3: ₹ / Notes
                        Box(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight()
                                .padding(start = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "₹ / Notes",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }

            // Table Row items for all days of the month
            itemsIndexed(
                items = monthDaysInfo,
                key = { _, it -> it.dateKey }
            ) { index, dayInfo ->
                val dayRecord = worker.attendance[dayInfo.dateKey]

                LaborAttendanceDayRow(
                    dayInfo = dayInfo,
                    isLast = index == monthDaysInfo.lastIndex,
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

    // Advance Amount Bottom Sheet for specific day (Matching reference screenshot exactly)
    selectedDayForAdvanceDialog?.let { day ->
        val dateKey = LaborCalendarHelper.getDateKey(currentYear, currentMonthNum, day)
        val dayRecord = worker.attendance[dateKey]

        AdvanceAmountBottomSheet(
            workerName = worker.name,
            day = day,
            selectedMonth = selectedMonth,
            initialAdvance = dayRecord?.advanceAmount ?: 0.0,
            initialNote = dayRecord?.note ?: "",
            initialPaymentMethod = PaymentMethod.CASH,
            onDismiss = { selectedDayForAdvanceDialog = null },
            onConfirm = { adv, note, paymentMethod ->
                viewModel.updateDayDetails(
                    workerId = worker.id,
                    dayNumber = day,
                    advance = adv,
                    note = note,
                    otHours = dayRecord?.overtimeHours ?: 0.0,
                    monthStr = selectedMonth
                )
                selectedDayForAdvanceDialog = null
            }
        )
    }

    // Overtime Bottom Sheet for specific day
    selectedDayForOvertimeDialog?.let { day ->
        val dateKey = LaborCalendarHelper.getDateKey(currentYear, currentMonthNum, day)
        val dayRecord = worker.attendance[dateKey]
        val formattedDateHeader = try {
            val monthName = selectedMonth.split(" ").firstOrNull() ?: "Aug"
            val yearName = selectedMonth.split(" ").lastOrNull() ?: "2026"
            "${monthName.take(3)} ${String.format("%02d", day)}, $yearName"
        } catch (e: Exception) {
            "Aug ${String.format("%02d", day)}, 2026"
        }

        val initialTotalOt = dayRecord?.overtimeHours ?: 0.0
        var otHours by remember { mutableIntStateOf(initialTotalOt.toInt()) }
        var otMinutes by remember { mutableIntStateOf(((initialTotalOt - initialTotalOt.toInt()) * 60).roundToInt()) }
        
        val defaultRate = (worker.dailyWage / 8.0).roundToInt().coerceAtLeast(0)
        var hourlyRateStr by remember { mutableStateOf(if (defaultRate > 0) defaultRate.toString() else "") }
        var showHoursPickerDialog by remember { mutableStateOf(false) }

        val currentTotalHours = otHours + (otMinutes / 60.0)
        val currentRate = hourlyRateStr.toDoubleOrNull() ?: defaultRate.toDouble()
        val totalOvertimeAmount = (currentTotalHours * currentRate).roundToInt()

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { selectedDayForOvertimeDialog = null },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // Header Row: Overtime (left) & Aug 01, 2026 (right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 56.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overtime",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = formattedDateHeader,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Hours Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    ) {
                        Text(
                            text = "Hours",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6B7280)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Clickable Hours Field (00:00 hrs)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF3F4F6))
                                .clickable { showHoursPickerDialog = true }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val hoursDisplay = String.format("%02d:%02d hrs", otHours, otMinutes)
                            Text(
                                text = hoursDisplay,
                                fontSize = 14.sp,
                                fontWeight = if (otHours > 0 || otMinutes > 0) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (otHours > 0 || otMinutes > 0) Color(0xFF111827) else Color(0xFF9CA3AF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Overtime Rate (Hourly) Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    ) {
                        Text(
                            text = "Overtime Rate (Hourly)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6B7280)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Hourly Rate Input Field
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "₹ ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                            BasicTextField(
                                value = hourlyRateStr,
                                onValueChange = { hourlyRateStr = it.filter { char -> char.isDigit() } },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF111827)
                                ),
                                decorationBox = { innerTextField ->
                                    if (hourlyRateStr.isEmpty()) {
                                        Text(
                                            text = "Enter Overtime rate",
                                            fontSize = 14.sp,
                                            color = Color(0xFF9CA3AF)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Total Overtime Amount Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Overtime Amount",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "₹$totalOvertimeAmount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Bottom Ok Button
                    val isOkActive = currentTotalHours > 0 || totalOvertimeAmount > 0
                    Button(
                        onClick = {
                            viewModel.updateDayDetails(
                                workerId = worker.id,
                                dayNumber = day,
                                advance = dayRecord?.advanceAmount ?: 0.0,
                                note = dayRecord?.note ?: "",
                                otHours = currentTotalHours,
                                monthStr = selectedMonth
                            )
                            selectedDayForOvertimeDialog = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOkActive) Color(0xFF1D61D2) else Color(0xFFE5E7EB),
                            contentColor = if (isOkActive) Color.White else Color(0xFF9CA3AF)
                        )
                    ) {
                        Text(
                            text = "Ok",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Floating Close Button (top right, above the sheet)
                IconButton(
                    onClick = { selectedDayForOvertimeDialog = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 12.dp, top = 2.dp)
                        .size(36.dp)
                        .shadow(3.dp, CircleShape)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF1F2937),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Hours and Minutes Picker Scroll Wheel Bottom Sheet
        if (showHoursPickerDialog) {
            OvertimeHoursPickerBottomSheet(
                initialHours = otHours,
                initialMinutes = otMinutes,
                onDismiss = { showHoursPickerDialog = false },
                onConfirm = { h, m ->
                    otHours = h
                    otMinutes = m
                    showHoursPickerDialog = false
                }
            )
        }
    }

    // Month Selector Bottom Sheet (Matching Screenshot exact design)
    if (showCalendarDialog) {
        MonthYearSelectionBottomSheet(
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

    // Edit Worker Profile Bottom Sheet (Matching reference screenshot exactly)
    if (showEditWorkerDialog) {
        EditWorkerProfileBottomSheet(
            worker = worker,
            onDismiss = { showEditWorkerDialog = false },
            onSave = { newName, newSalary, salaryType ->
                viewModel.updateWorker(worker.id, newName, worker.phoneNumber, newSalary)
                showEditWorkerDialog = false
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
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color.White)
                        .padding(top = 16.dp)
                ) {
                    // Header: Worker Name & Date
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = worker.name,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "Mark Attendance",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF6B7280)
                            )
                        }

                        Text(
                            text = dateDisplayString,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            modifier = Modifier.padding(end = 36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Attendance Status Pills (Row 1 & Row 2 exactly matching screenshot)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Row 1: A, 1/2, P, P + 1/2, P + P, OT
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AttendanceSheetPill(
                                modifier = Modifier.weight(1f),
                                label = "A",
                                isSelected = currentStatus == AttendanceStatus.ABSENT,
                                selectedColor = Color(0xFFE52323),
                                onClick = { currentStatus = AttendanceStatus.ABSENT }
                            )
                            AttendanceSheetPill(
                                modifier = Modifier.weight(1.1f),
                                label = "1/2",
                                isSelected = currentStatus == AttendanceStatus.HALF_DAY,
                                selectedColor = Color(0xFF2E9B66),
                                onClick = { currentStatus = AttendanceStatus.HALF_DAY }
                            )
                            AttendanceSheetPill(
                                modifier = Modifier.weight(1f),
                                label = "P",
                                isSelected = currentStatus == AttendanceStatus.PRESENT,
                                selectedColor = Color(0xFF2E9B66),
                                onClick = { currentStatus = AttendanceStatus.PRESENT }
                            )
                            AttendanceSheetPill(
                                modifier = Modifier.weight(1.4f),
                                label = "P + 1/2",
                                isSelected = currentStatus == AttendanceStatus.PRESENT_HALF,
                                selectedColor = Color(0xFF2E9B66),
                                onClick = { currentStatus = AttendanceStatus.PRESENT_HALF }
                            )
                            AttendanceSheetPill(
                                modifier = Modifier.weight(1.3f),
                                label = "P + P",
                                isSelected = currentStatus == AttendanceStatus.DOUBLE,
                                selectedColor = Color(0xFF2E9B66),
                                onClick = { currentStatus = AttendanceStatus.DOUBLE }
                            )

                            // OT Pill (Mauve Purple outline / rounded)
                            val isOt = (dayRecord?.overtimeHours ?: 0.0) > 0 || currentStatus == AttendanceStatus.OVERTIME
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = 1.2.dp,
                                        color = Color(0xFF7E3B7D),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(if (isOt) Color(0xFF7E3B7D) else Color.White)
                                    .clickable {
                                        selectedDayForAttendanceSheet = null
                                        selectedDayForOvertimeDialog = day
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if ((dayRecord?.overtimeHours ?: 0.0) > 0) "${if (dayRecord!!.overtimeHours % 1.0 == 0.0) dayRecord.overtimeHours.toInt() else dayRecord.overtimeHours}h" else "OT",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOt) Color.White else Color(0xFF7E3B7D)
                                )
                            }
                        }

                        // Row 2: PA (Paid Leave)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            AttendanceSheetPill(
                                modifier = Modifier.width(68.dp),
                                label = "PA",
                                isSelected = currentStatus == AttendanceStatus.PAID_LEAVE,
                                selectedColor = Color(0xFF5E3FB5),
                                onClick = { currentStatus = AttendanceStatus.PAID_LEAVE }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MEANING Section (Exact match from screenshot with light warm off-white background)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAF9F6))
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "MEANING:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MeaningItem(boldKey = "A", desc = " - Absent")
                            MeaningItem(boldKey = "½", desc = " - Half day")
                            MeaningItem(boldKey = "P", desc = " - Present")
                            MeaningItem(boldKey = "OT", desc = " - Overtime")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MeaningItem(boldKey = "P + ½", desc = " - 1.5 day")
                            Spacer(modifier = Modifier.width(22.dp))
                            MeaningItem(boldKey = "P+P", desc = " - Double")
                            Spacer(modifier = Modifier.width(22.dp))
                            MeaningItem(boldKey = "PA", desc = " - Paid Leave")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Action Buttons: Remove Marked and Ok
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Remove Marked Button (Outlined pill)
                        OutlinedButton(
                            onClick = {
                                viewModel.setAttendance(worker.id, day, AttendanceStatus.UNMARKED, selectedMonth)
                                selectedDayForAttendanceSheet = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF1D61D2)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1D61D2)
                            )
                        ) {
                            Text(
                                text = "Remove Marked",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D61D2)
                            )
                        }

                        // Ok Button (Solid filled royal blue pill)
                        Button(
                            onClick = {
                                viewModel.setAttendance(worker.id, day, currentStatus, selectedMonth)
                                selectedDayForAttendanceSheet = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1D61D2),
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

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Floating Close Button (top right, above the sheet background)
                IconButton(
                    onClick = { selectedDayForAttendanceSheet = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 0.dp, end = 12.dp)
                        .size(36.dp)
                        .shadow(3.dp, CircleShape)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF1F2937),
                        modifier = Modifier.size(18.dp)
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
    isLast: Boolean = false,
    status: AttendanceStatus,
    advance: Double,
    note: String,
    otHours: Double,
    onStatusSelected: (Int, AttendanceStatus) -> Unit,
    onOvertimeClicked: (Int) -> Unit,
    onAdvanceClicked: (Int) -> Unit,
    onOpenAttendanceSheet: (Int, com.example.domain.model.AttendanceStatus?) -> Unit
) {
    val isSunday = dayInfo.dow.equals("Sun", ignoreCase = true)
    val isSaturday = dayInfo.dow.equals("Sat", ignoreCase = true)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = if (isLast) RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp) else RoundedCornerShape(0.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        shadowElevation = if (isLast) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Col 1: Date
            Column(
                modifier = Modifier
                    .width(68.dp)
                    .fillMaxHeight()
                    .background(if (isSunday) Color(0xFFFFF1F2) else if (isSaturday) Color(0xFFF0F9FF) else Color.Transparent),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val dayNumStr = if (dayInfo.day < 10) "0${dayInfo.day}" else "${dayInfo.day}"
                Text(
                    text = dayNumStr,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isSunday -> Color(0xFFE11D48)
                        isSaturday -> Color(0xFF0284C7)
                        else -> Color(0xFF0F172A)
                    }
                )
                Text(
                    text = dayInfo.dow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isSunday -> Color(0xFFF43F5E)
                        isSaturday -> Color(0xFF38BDF8)
                        else -> Color(0xFF64748B)
                    }
                )
            }
            // Vertical divider 1
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFCBD5E1)))
            
            // Col 2: Attendance
            Row(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (status) {
                        AttendanceStatus.ABSENT -> {
                            // Solid Red A
                            AttendancePillButton(
                                label = "A",
                                isSelected = true,
                                activeColor = Color(0xFFE52323),
                                onClick = { onOpenAttendanceSheet(dayInfo.day, AttendanceStatus.ABSENT) }
                            )
                        }
                        AttendanceStatus.PRESENT -> {
                            // Solid Green P
                            AttendancePillButton(
                                label = "P",
                                isSelected = true,
                                activeColor = Color(0xFF2E9B66),
                                onClick = { onOpenAttendanceSheet(dayInfo.day, AttendanceStatus.PRESENT) }
                            )
                        }
                        AttendanceStatus.HALF_DAY -> {
                            // Solid Green 1/2
                            AttendancePillButton(
                                label = "1/2",
                                isSelected = true,
                                activeColor = Color(0xFF2E9B66),
                                onClick = { onOpenAttendanceSheet(dayInfo.day, AttendanceStatus.HALF_DAY) }
                            )
                        }
                        AttendanceStatus.PRESENT_HALF -> {
                            // Solid Green P + 1/2
                            AttendancePillButton(
                                label = "P + 1/2",
                                isSelected = true,
                                activeColor = Color(0xFF2E9B66),
                                onClick = { onOpenAttendanceSheet(dayInfo.day, AttendanceStatus.PRESENT_HALF) }
                            )
                        }
                        AttendanceStatus.DOUBLE -> {
                            // Solid Green P + P
                            AttendancePillButton(
                                label = "P + P",
                                isSelected = true,
                                activeColor = Color(0xFF2E9B66),
                                onClick = { onOpenAttendanceSheet(dayInfo.day, AttendanceStatus.DOUBLE) }
                            )
                        }
                        AttendanceStatus.PAID_LEAVE -> {
                            // Solid Purple PA
                            AttendancePillButton(
                                label = "PA",
                                isSelected = true,
                                activeColor = Color(0xFF5E3FB5),
                                onClick = { onOpenAttendanceSheet(dayInfo.day, AttendanceStatus.PAID_LEAVE) }
                            )
                        }
                        else -> {
                            // UNMARKED: show [A] (red outline) and [P] (green outline)
                            AttendancePillButton(
                                label = "A",
                                isSelected = false,
                                activeColor = Color(0xFFE52323),
                                onClick = { onStatusSelected(dayInfo.day, AttendanceStatus.ABSENT) }
                            )
                            AttendancePillButton(
                                label = "P",
                                isSelected = false,
                                activeColor = Color(0xFF2E9B66),
                                onClick = { onStatusSelected(dayInfo.day, AttendanceStatus.PRESENT) }
                            )
                        }
                    }

                    // OT Pill (Mauve Purple outline / rounded with shadow)
                    val isOtActive = otHours > 0 || status == AttendanceStatus.OVERTIME
                    Surface(
                        modifier = Modifier
                            .height(30.dp)
                            .widthIn(min = 34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOvertimeClicked(dayInfo.day) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            width = 1.2.dp,
                            color = Color(0xFF7E3B7D)
                        ),
                        color = if (isOtActive) Color(0xFF7E3B7D) else Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (otHours > 0) "${if (otHours % 1.0 == 0.0) otHours.toInt() else otHours}h" else "OT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOtActive) Color.White else Color(0xFF7E3B7D)
                            )
                        }
                    }
                }

                // 3 dots More Menu
                IconButton(
                    onClick = { onOpenAttendanceSheet(dayInfo.day, status) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Vertical divider 2
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFCBD5E1)))
            
            // Col 3: ₹ / Notes
            Row(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .clickable { onAdvanceClicked(dayInfo.day) }
                    .padding(start = 12.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Showing ₹ symbol or Advance Amount on left
                if (advance > 0) {
                    val formattedAdvance = if (advance % 1.0 == 0.0) advance.toInt().toString() else advance.toString()
                    Text(
                        text = "₹ $formattedAdvance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626) // Clean Red color for Advance
                    )
                } else {
                    Text(
                        text = "₹",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }
                
                // > icon on right
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Advance & Notes",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AttendanceSheetPill(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    selectedColor: Color = Color(0xFF1D61D2),
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.2.dp,
                color = if (isSelected) selectedColor else Color(0xFFE5E7EB),
                shape = RoundedCornerShape(8.dp)
            )
            .background(if (isSelected) selectedColor else Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color(0xFF111827)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(30.dp)
            .widthIn(min = 30.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.2.dp,
            color = activeColor
        ),
        color = if (isSelected) activeColor else Color.White,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else activeColor
            )
        }
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
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Swipe to delete",
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OvertimeNumberWheel(
    count: Int,
    initialValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: androidx.compose.ui.unit.Dp = 44.dp
) {
    val totalLoops = 1000
    val totalItems = count * totalLoops
    val initialIndex = (totalLoops / 2) * count + (initialValue % count)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (initialIndex - 1).coerceAtLeast(0))
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }

    val selectedValue by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val centerIndex = if (offset > itemHeightPx / 2) first + 2 else first + 1
            ((centerIndex % count) + count) % count
        }
    }

    androidx.compose.runtime.LaunchedEffect(selectedValue) {
        onValueChange(selectedValue)
    }

    Box(
        modifier = modifier
            .width(62.dp)
            .height(itemHeight * 3),
        contentAlignment = Alignment.Center
    ) {
        // Selection indicator lines (above & below center item)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(Color(0xFF6B7280))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(Color(0xFF6B7280))
            )
        }

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(totalItems) { index ->
                val num = index % count
                val isSelected = num == selectedValue
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", num),
                        fontSize = if (isSelected) 22.sp else 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF111827) else Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvertimeHoursPickerBottomSheet(
    initialHours: Int,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selectedHours by remember { mutableIntStateOf(initialHours.coerceIn(0, 23)) }
    var selectedMinutes by remember { mutableIntStateOf(initialMinutes.coerceIn(0, 59)) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Title
                Text(
                    text = "Overtime Hours",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Spacer(modifier = Modifier.height(44.dp))

                // Scroll Wheel Row: Hrs [ 00 ] : [ 00 ] Mins
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hrs",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    OvertimeNumberWheel(
                        count = 24,
                        initialValue = selectedHours,
                        onValueChange = { selectedHours = it }
                    )

                    Text(
                        text = ":",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    OvertimeNumberWheel(
                        count = 60,
                        initialValue = selectedMinutes,
                        onValueChange = { selectedMinutes = it }
                    )

                    Text(
                        text = "Mins",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                }

                Spacer(modifier = Modifier.height(44.dp))

                // Ok Button
                Button(
                    onClick = {
                        onConfirm(selectedHours, selectedMinutes)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedHours > 0 || selectedMinutes > 0) Color(0xFF1D61D2) else Color(0xFFB5B8BE),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Ok",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Floating Close Button (top-right)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 16.dp)
                    .size(38.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF111827),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

