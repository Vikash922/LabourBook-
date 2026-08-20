import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Update Header Left and Right borders
old_header_row = """                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {"""
new_header_row = """                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(Color.White)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left outer border
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))"""
                    
# Wait, I also need to close the right side of the header
# Find the Col 3 box in the header
old_header_col3 = """                    // Col 3: ₹ / Notes
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
                }"""

new_header_col3 = """                    // Col 3: ₹ / Notes
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
                }"""

# Update Horizontal dividers to also have padding if needed. 
# Oh wait, the `fillMaxWidth()` dividers will stretch to the screen edges. 
# If I add padding to the Row, I should also add padding to the dividers.
# Let's see the old header dividers.
old_header_div = """            item {
                HorizontalDivider(color = Color.Black, thickness = 2.dp)"""
new_header_div = """            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = Color.Black, thickness = 2.dp)"""

old_header_div2 = """                HorizontalDivider(color = Color.Black, thickness = 2.dp)
            }"""
new_header_div2 = """                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = Color.Black, thickness = 2.dp)
            }"""

# Update LaborAttendanceDayRow similarly
old_row = """    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {"""
new_row = """    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left outer border
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))"""

old_row_col3 = """                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Edit Note",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider(color = Color.Black, thickness = 2.dp)
    }"""
new_row_col3 = """                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Edit Note",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            // Right outer border
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
        }
        HorizontalDivider(color = Color.Black, thickness = 2.dp)
    }"""

content = content.replace(old_header_row, new_header_row)
content = content.replace(old_header_col3, new_header_col3)
content = content.replace(old_header_div, new_header_div)
content = content.replace(old_header_div2, new_header_div2)

content = content.replace(old_row, new_row)
content = content.replace(old_row_col3, new_row_col3)

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated table borders")
