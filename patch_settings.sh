#!/bin/bash
# Rename variables
sed -i 's/showDriveBackupSheet/showCloudBackupSheet/g' /app/applet/app/src/main/java/com/example/ui/screens/SettingsScreen.kt
sed -i 's/logoutWithDriveBackup/logoutWithCloudBackup/g' /app/applet/app/src/main/java/com/example/ui/screens/SettingsScreen.kt
sed -i 's/viewModel.backupToGoogleDrive(context)/viewModel.backupToCloudNow()/g' /app/applet/app/src/main/java/com/example/ui/screens/SettingsScreen.kt

# Remove the file picker and the local import option. We can just use sed -i to remove the blocks.
# Let's see what happens if I remove it.
