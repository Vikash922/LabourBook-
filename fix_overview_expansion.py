import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Add states at the top
states_injection = """    var selectedTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var isOverviewExpanded by remember { mutableStateOf(false) }
    var isRefreshingOverview by remember { mutableStateOf(false) }
    val rotationAngle by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isRefreshingOverview) 360f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing),
        finishedListener = { isRefreshingOverview = false },
        label = "refresh_rotation"
    )"""
content = content.replace("    var selectedTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }", states_injection)

# Replace the chevron icon with an IconButton and expansion toggle
old_chevron = """                    // 5. Chevron Arrow (Right edge)
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                    )"""

new_chevron = """                    // 5. Chevron Arrow (Right edge)
                    IconButton(onClick = { isOverviewExpanded = !isOverviewExpanded }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isOverviewExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                    }"""
content = content.replace(old_chevron, new_chevron)

# Add expanded content just below the KPI row
expanded_content = """                }
            }
            
            // Expanded Overview Section
            if (isOverviewExpanded) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FAFB))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estimated Month Earnings: ₹${if (monthEstimatedEarnings % 1.0 == 0.0) monthEstimatedEarnings.toInt() else monthEstimatedEarnings}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { isRefreshingOverview = true }
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = LaborBlue,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .androidx.compose.ui.draw.rotate(rotationAngle)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Refresh",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborBlue
                                )
                            }
                        }
                    }
                }
            }
"""
content = content.replace("                }\n            }\n            // Open Report Banner", expanded_content + "\n            // Open Report Banner")

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated Overview expansion")
