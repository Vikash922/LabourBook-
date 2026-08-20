import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Fix Col 1
old_col1 = """            // Col 1: Date
            Column(
                modifier = Modifier
                    .width(70.dp)
                    .padding(vertical = 12.dp),"""
new_col1 = """            // Col 1: Date
            Column(
                modifier = Modifier
                    .width(55.dp)
                    .padding(vertical = 4.dp),"""
content = content.replace(old_col1, new_col1)

# Fix Col 2
old_col2 = """            // Col 2: Attendance
            Row(
                modifier = Modifier
                    .weight(1.2f)
                    .padding(vertical = 12.dp, horizontal = 4.dp),"""
new_col2 = """            // Col 2: Attendance
            Row(
                modifier = Modifier
                    .weight(1.3f)
                    .padding(vertical = 4.dp, horizontal = 4.dp),"""
content = content.replace(old_col2, new_col2)

# Fix Col 2 spacing
content = content.replace("horizontalArrangement = Arrangement.spacedBy(6.dp),", "horizontalArrangement = Arrangement.spacedBy(4.dp),")

# Replace the 3 dots section entirely
old_3dots_pattern = re.compile(r'// 3 dots\s*var showRowMenu.*?\}\s*\}', re.DOTALL)
new_3dots = """// 3 dots
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )"""
# Wait, I need to make sure I only replace the row 3 dots.
content = old_3dots_pattern.sub(new_3dots, content, count=1)

# Fix Col 3
old_col3 = """            // Col 3: ₹ / Notes
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onAdvanceClicked(dayInfo.day) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),"""
new_col3 = """            // Col 3: ₹ / Notes
            Row(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .clickable { onAdvanceClicked(dayInfo.day) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),"""
content = content.replace(old_col3, new_col3)

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated table rows and 3 dots")
