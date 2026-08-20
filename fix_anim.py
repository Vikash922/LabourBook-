import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Fix fully qualified animateFloatAsState
content = content.replace(
    'androidx.compose.animation.core.animateFloatAsState',
    'animateFloatAsState'
)
content = content.replace(
    'androidx.compose.animation.core.tween',
    'tween'
)
content = content.replace(
    'androidx.compose.animation.core.LinearEasing',
    'LinearEasing'
)

# Add imports
imports = """import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
"""
if 'import androidx.compose.animation.core.animateFloatAsState' not in content:
    content = content.replace('import androidx.compose.runtime.Composable', imports + 'import androidx.compose.runtime.Composable')

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated anim")
