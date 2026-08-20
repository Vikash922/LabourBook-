import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Define the start and end of LaborAttendanceDayRow
start_str = """@Composable
fun LaborAttendanceDayRow("""
end_str = """        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
    }
}"""

# Find start and end indices
start_idx = content.find(start_str)
end_idx = content.find(end_str, start_idx) + len(end_str)

new_row = """@Composable
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Col 1: Date
            Column(
                modifier = Modifier
                    .width(70.dp)
                    .padding(vertical = 12.dp),
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
                    .weight(1.2f)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                            .height(28.dp)
                            .widthIn(min = 30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = 1.dp,
                                color = if (isOtActive) Color(0xFF8B5CF6) else Color.Gray,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(if (isOtActive) Color(0xFF8B5CF6) else Color.White)
                            .clickable {
                                if (isOtActive) onOpenAttendanceSheet(dayInfo.day, null)
                                else onOvertimeClicked(dayInfo.day)
                            }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (otHours > 0) "${if (otHours % 1.0 == 0.0) otHours.toInt() else otHours}h" else "OT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOtActive) Color.White else Color.Gray
                        )
                    }
                }

                // 3 dots
                var showRowMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showRowMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showRowMenu,
                        onDismissRequest = { showRowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Log Overtime") },
                            onClick = {
                                onOvertimeClicked(dayInfo.day)
                                showRowMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Advance / Note") },
                            onClick = {
                                onAdvanceClicked(dayInfo.day)
                                showRowMenu = false
                            }
                        )
                    }
                }
            }

            // Vertical divider 2
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
            
            // Col 3: ₹ / Notes
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onAdvanceClicked(dayInfo.day) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
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
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Edit Note",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider(color = Color.Black, thickness = 2.dp)
    }
}"""

content = content[:start_idx] + new_row + content[end_idx:]

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated table row")
