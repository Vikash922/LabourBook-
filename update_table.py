import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Update Table Header
old_header = """            // Table Header: Date | Attendance | ₹ / Notes with vertical grid lines
            item {
                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F6))
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Col 1: Date
                    Box(
                        modifier = Modifier
                            .width(62.dp)
                            .padding(start = 14.dp)
                    ) {
                        Text(
                            text = "Date",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }
                    // Vertical divider 1
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(Color(0xFFD1D5DB))
                    )
                    // Col 2: Attendance
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = "Attendance",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }
                    // Vertical divider 2
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(Color(0xFFD1D5DB))
                    )
                    // Col 3: ₹ / Notes
                    Box(
                        modifier = Modifier
                            .weight(1.6f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = "₹ / Notes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
            }"""

new_header = """            // Table Header: Date | Attendance | ₹ / Notes with vertical grid lines
            item {
                HorizontalDivider(color = Color.Black, thickness = 2.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Col 1: Date
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .padding(vertical = 12.dp),
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
                            .weight(1.2f)
                            .padding(vertical = 12.dp),
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
                            .weight(1f)
                            .padding(start = 8.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "₹/Notes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                    }
                }
                HorizontalDivider(color = Color.Black, thickness = 2.dp)
            }"""
content = content.replace(old_header, new_header)

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated table header")
