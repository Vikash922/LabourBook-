package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.LaborCalendarHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthYearSelectionBottomSheet(
    initialSelection: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val (initialYear, initialMonth) = remember(initialSelection) {
        LaborCalendarHelper.parseYearMonth(initialSelection)
    }

    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonthNum by remember { mutableIntStateOf(initialMonth) } // 1..12

    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }

    val monthFullName = LaborCalendarHelper.monthsFull.getOrElse(selectedMonthNum - 1) { "August" }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                    text = "Select Month & Year",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Selector Pills Row: [ 📅 August ⌵ ]  [ 2026 ⌵ ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month Pill
                    Surface(
                        modifier = Modifier
                            .weight(1.15f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .clickable { showMonthPicker = true },
                        shape = RoundedCornerShape(26.dp),
                        color = Color.White,
                        border = BorderStroke(1.2.dp, Color(0xFFD1D5DB))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = monthFullName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Year Pill
                    Surface(
                        modifier = Modifier
                            .weight(0.85f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .clickable { showYearPicker = true },
                        shape = RoundedCornerShape(26.dp),
                        color = Color.White,
                        border = BorderStroke(1.2.dp, Color(0xFFD1D5DB))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedYear.toString(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Ok Button (Blue pill)
                Button(
                    onClick = {
                        val formatted = LaborCalendarHelper.formatYearMonth(selectedYear, selectedMonthNum)
                        onConfirm(formatted)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1D61D2),
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

    // Month Picker Dialog
    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Select Month",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(LaborCalendarHelper.monthsFull.indices.toList()) { index ->
                        val mNum = index + 1
                        val mName = LaborCalendarHelper.monthsFull[index]
                        val isSelected = mNum == selectedMonthNum

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedMonthNum = mNum
                                    showMonthPicker = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedMonthNum = mNum
                                    showMonthPicker = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF1D61D2),
                                    unselectedColor = Color(0xFF9CA3AF)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = mName,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF1D61D2) else Color(0xFF1F2937)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMonthPicker = false }) {
                    Text("Cancel", color = Color(0xFF1D61D2), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Year Picker Dialog
    if (showYearPicker) {
        val yearsList = (2020..2035).toList()
        AlertDialog(
            onDismissRequest = { showYearPicker = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Select Year",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(yearsList) { y ->
                        val isSelected = y == selectedYear
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedYear = y
                                    showYearPicker = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedYear = y
                                    showYearPicker = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF1D61D2),
                                    unselectedColor = Color(0xFF9CA3AF)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = y.toString(),
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF1D61D2) else Color(0xFF1F2937)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showYearPicker = false }) {
                    Text("Cancel", color = Color(0xFF1D61D2), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
