import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Using regex to find the Table Header block and replace it
header_pattern = re.compile(r'// Table Header.*?HorizontalDivider\(color = Color\(0xFFE5E7EB\), thickness = 1\.dp\)\s*\}', re.DOTALL)

new_header = """// Table Header: Date | Attendance | ₹ / Notes with vertical grid lines
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = Color.Black, thickness = 2.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(Color.White)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left outer border
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                    
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
                    
                    // Right outer border
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = Color.Black, thickness = 2.dp)
            }"""

if header_pattern.search(content):
    content = header_pattern.sub(new_header, content)
    with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
        f.write(content)
    print("Header successfully replaced")
else:
    print("Header not found")
