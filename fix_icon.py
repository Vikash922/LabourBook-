import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Replace KeyboardArrowUp usage
old = "if (isOverviewExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown"
new = "Icons.Default.KeyboardArrowDown"
content = content.replace(old, new)

# Add rotation
old_mod = 'modifier = Modifier.size(24.dp)'
new_mod = 'modifier = Modifier.size(24.dp).androidx.compose.ui.draw.rotate(if (isOverviewExpanded) 180f else 0f)'
content = content.replace(
    '                            tint = Color(0xFF2563EB),\n                            modifier = Modifier.size(24.dp)',
    f'                            tint = Color(0xFF2563EB),\n                            {new_mod}'
)

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated Icon")
