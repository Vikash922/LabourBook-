import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Make Pills smaller so they all fit!
old_pill = """fun AttendancePillButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .widthIn(min = 24.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                color = activeColor,
                shape = RoundedCornerShape(4.dp)
            )
            .background(if (isSelected) activeColor else Color.White)
            .clickable { onClick() }
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else activeColor
        )
    }
}"""
new_pill = """fun AttendancePillButton(
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
}"""
content = content.replace(old_pill, new_pill)

# Make sure Col 2 uses weight(1f) for the pills container so 3 dots are never pushed out
old_col2 = """            // Col 2: Attendance
            Row(
                modifier = Modifier
                    .weight(1.3f)
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {"""
new_col2 = """            // Col 2: Attendance
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
                ) {"""
content = content.replace(old_col2, new_col2)

# Fix the OT pill inside Col 2
old_ot_pill = """                    Box(
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
                            fontSize = 11.sp,"""
new_ot_pill = """                    Box(
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
                            fontSize = 10.sp,"""
content = content.replace(old_ot_pill, new_ot_pill)

# Make 3 dots explicitly NOT clickable (it already is just an Icon, but I'll make it smaller to fit better)
old_3dots = """                // 3 dots
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )"""
new_3dots = """                // 3 dots
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )"""
content = content.replace(old_3dots, new_3dots)

# I will also change IntrinsicSize.Min to a fixed height to avoid any weird LazyColumn spacing bugs
old_row = """        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {"""
new_row = """        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {"""
content = content.replace(old_row, new_row)

old_header_row = """                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(Color.White)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {"""
new_header_row = """                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(Color.White)
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {"""
content = content.replace(old_header_row, new_header_row)

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated table rows and 3 dots")
