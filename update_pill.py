import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

old_pill = """fun AttendancePillButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
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
            .padding(horizontal = 4.dp)
            .testTag("attendance_pill_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
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
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else activeColor
        )
    }
}"""

# I need to find the actual pill function and replace it. 
# Let's use regex to replace it properly.
pill_pattern = re.compile(r'fun AttendancePillButton\(.*?\}\s*\}', re.DOTALL)
content = pill_pattern.sub(new_pill, content, count=1)

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated pill button")
