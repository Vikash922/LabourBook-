import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Fix fully qualified rotate
content = content.replace(
    '.androidx.compose.ui.draw.rotate(rotationAngle)',
    '.rotate(rotationAngle)'
)

# Fix other occurrences just in case
content = content.replace(
    '.androidx.compose.ui.draw.rotate(if (isOverviewExpanded) 180f else 0f)',
    '.rotate(if (isOverviewExpanded) 180f else 0f)'
)

# Add import if missing
if 'import androidx.compose.ui.draw.rotate' not in content:
    content = content.replace('import androidx.compose.ui.draw.clip', 'import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.rotate')

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated rotate")
