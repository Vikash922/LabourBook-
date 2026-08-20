import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Fix Col 1 in header
old_col1 = """                    // Col 1: Date
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .padding(vertical = 12.dp),"""
new_col1 = """                    // Col 1: Date
                    Box(
                        modifier = Modifier
                            .width(55.dp)
                            .padding(vertical = 6.dp),"""
content = content.replace(old_col1, new_col1)

# Fix Col 2 in header
old_col2 = """                    // Col 2: Attendance
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .padding(vertical = 12.dp),"""
new_col2 = """                    // Col 2: Attendance
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .padding(vertical = 6.dp),"""
content = content.replace(old_col2, new_col2)

# Fix Col 3 in header
old_col3 = """                    // Col 3: ₹ / Notes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp, top = 12.dp, bottom = 12.dp),"""
new_col3 = """                    // Col 3: ₹ / Notes
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),"""
content = content.replace(old_col3, new_col3)

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated table header widths and paddings")
