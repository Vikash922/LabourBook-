import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Make sure IntrinsicSize is imported
if 'import androidx.compose.foundation.layout.IntrinsicSize' not in content:
    content = content.replace('import androidx.compose.foundation.layout.Row', 'import androidx.compose.foundation.layout.Row\nimport androidx.compose.foundation.layout.IntrinsicSize')

# Check for AttendanceStatus if missing in the new row
# Wait, let's see if the build failed due to IntrinsicSize

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated IntrinsicSize")
