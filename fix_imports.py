import os
import re

base_dir = "app/src/main/java/com/example"

replacements = {
    "com.example.ui.theme": "com.example.presentation.theme",
    "com.example.ui.components": "com.example.presentation.components",
    "com.example.ui.screens": "com.example.presentation.screens",
    "com.example.ui.viewmodel": "com.example.presentation.viewmodel",
    "com.example.data.cloud": "com.example.data.remote",
    "com.example.data.model": "com.example.domain.model",
    "com.example.util": "com.example.core.util"
}

for root, dirs, files in os.walk(base_dir):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, "r") as f:
                content = f.read()
            
            modified = False
            for old, new in replacements.items():
                if old in content:
                    content = content.replace(old, new)
                    modified = True
            
            if modified:
                with open(filepath, "w") as f:
                    f.write(content)
                print(f"Updated {filepath}")

# Also check MainActivity
main_activity = os.path.join(base_dir, "MainActivity.kt")
if os.path.exists(main_activity):
    with open(main_activity, "r") as f:
        content = f.read()
    
    modified = False
    for old, new in replacements.items():
        if old in content:
            content = content.replace(old, new)
            modified = True
    
    if modified:
        with open(main_activity, "w") as f:
            f.write(content)
        print("Updated MainActivity.kt")

print("Imports fixed")
