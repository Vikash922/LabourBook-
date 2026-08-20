import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Change Overview header background to very light gray
content = content.replace(
    '.background(Color(0xFFD1D5DB))',
    '.background(Color(0xFFF9FAFB))',
    1 # Only the first occurrence which is the Overview header
)

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated colors")
