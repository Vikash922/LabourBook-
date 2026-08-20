import re

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "r") as f:
    content = f.read()

# Fix gap in Col 1 (Date)
content = content.replace('.padding(vertical = 0.dp),', '.padding(vertical = 4.dp),')
content = content.replace('.padding(vertical = 0.dp, horizontal = 4.dp),', '.padding(vertical = 4.dp, horizontal = 4.dp),')
content = content.replace('.padding(horizontal = 6.dp, vertical = 0.dp),', '.padding(horizontal = 6.dp, vertical = 4.dp),')

with open("app/src/main/java/com/example/presentation/screens/LaborDetailScreen.kt", "w") as f:
    f.write(content)
print("Updated padding")
