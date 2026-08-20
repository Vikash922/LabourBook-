import os
import shutil

base_dir = "app/src/main/java/com/example"

# Define mappings for directories (old -> new)
moves = {
    "ui/theme": "presentation/theme",
    "ui/components": "presentation/components",
    "ui/screens": "presentation/screens",
    "ui/viewmodel": "presentation/viewmodel",
    "data/cloud": "data/remote",
    "data/model": "domain/model",
    "util": "core/util"
}

# Create new directories
for old, new in moves.items():
    os.makedirs(os.path.join(base_dir, new), exist_ok=True)

# Move files
for old, new in moves.items():
    old_path = os.path.join(base_dir, old)
    new_path = os.path.join(base_dir, new)
    if os.path.exists(old_path):
        for item in os.listdir(old_path):
            s = os.path.join(old_path, item)
            d = os.path.join(new_path, item)
            shutil.move(s, d)
        # Remove old directory if empty
        try:
            os.rmdir(old_path)
        except OSError:
            pass

# Also remove old parent dirs if empty
try:
    os.rmdir(os.path.join(base_dir, "ui"))
except:
    pass

print("Folders moved successfully")
