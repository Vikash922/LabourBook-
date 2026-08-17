import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/CashBookScreen.kt', 'r') as f:
    content = f.read()

# Replace Search Bar unfocusedBorderColor
content = content.replace("unfocusedBorderColor = Color(0xFFE0E0E0)", "unfocusedBorderColor = Color(0xFF9CA3AF)")

# Replace Summary Card Border
content = content.replace("border = BorderStroke(1.dp, Color(0xFFE5E7EB))", "border = BorderStroke(1.dp, Color(0xFF9CA3AF))")

# Month Selector pill might have LaborDivider or E5E7EB
content = content.replace("border = BorderStroke(1.dp, LaborDivider)", "border = BorderStroke(1.dp, Color(0xFF9CA3AF))")

with open('/app/applet/app/src/main/java/com/example/ui/screens/CashBookScreen.kt', 'w') as f:
    f.write(content)
