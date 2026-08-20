import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Fix gap in Col 1 (Date)
content = content.replace('.padding(vertical = 4.dp),', '.padding(vertical = 0.dp),')

# Fix gap in Col 2 (Attendance)
content = content.replace('.padding(vertical = 4.dp, horizontal = 4.dp),', '.padding(vertical = 0.dp, horizontal = 4.dp),')

# Fix gap in Col 3 (₹/Notes)
content = content.replace('.padding(horizontal = 6.dp, vertical = 4.dp),', '.padding(horizontal = 6.dp, vertical = 0.dp),')

# Also fix the header padding to make it smaller
content = content.replace('.padding(vertical = 6.dp),', '.padding(vertical = 2.dp),')
content = content.replace('.padding(start = 8.dp, top = 6.dp, bottom = 6.dp),', '.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),')

# Make the pills smaller
old_pill_box = """    Box(
        modifier = Modifier
            .height(28.dp)
            .widthIn(min = 30.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = 1.dp,
                color = activeColor,
                shape = RoundedCornerShape(6.dp)
            )
            .background(if (isSelected) activeColor else Color.White)
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,"""
new_pill_box = """    Box(
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
            fontSize = 11.sp,"""
content = content.replace(old_pill_box, new_pill_box)

# Make the OT pill smaller in the row
old_ot_pill = """                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .widthIn(min = 30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = 1.dp,
                                color = Color(0xFF8B5CF6),
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
                            fontSize = 11.sp,"""
content = content.replace(old_ot_pill, new_ot_pill)


with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated pills and padding")
