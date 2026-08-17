import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# 1. Remove driveFilePicker
content = re.sub(r'val driveFilePicker = rememberLauncherForActivityResult\([\s\S]*?\}', '', content)

# 2. Remove Local File Actions (Import from File) in the cloud sheet
content = re.sub(r'SettingsGroupCard\(title = "Local File Actions"\) \{[\s\S]*?Spacer\(modifier = Modifier\.height\(32\.dp\)\)', 'Spacer(modifier = Modifier.height(32.dp))', content)

# 3. Remove "Import Backup File" from settings list
# It looks like:
# SettingsRowItem(
#     icon = Icons.Default.FileDownload,
#     title = "Import Backup File",
#     subtitle = "Restore data from a .CSV or .JSON file on phone",
#     iconTint = LaborBlue,
#     onClick = { driveFilePicker.launch("*/*") }
# )
content = re.sub(r'SettingsRowItem\(\s*icon = Icons\.Default\.FileDownload,\s*title = "Import Backup File"[\s\S]*?onClick = \{ driveFilePicker\.launch\("\*/\*"\) \}\s*\)', '', content)

# 4. Remove backupToRestoreConfirm logic (lines 499 to 574 approx)
content = re.sub(r'var backupToRestoreConfirm by remember \{ mutableStateOf<BackupMetadata\?>\(null\) \}[\s\S]*?if \(backupToRestoreConfirm != null\) \{[\s\S]*?\}\s*\}\s*\)\s*\}', '', content)

with open('/app/applet/app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
