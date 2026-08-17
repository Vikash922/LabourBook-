#!/bin/bash
sed -i '/import com.example.data.cloud.GoogleDriveBackupService/d' /app/applet/app/src/main/java/com/example/ui/viewmodel/LaborViewModel.kt

sed -i 's/val safetyBackup = GoogleDriveBackupService.getLatestBackupForUser(getApplication(), email)/val safetyBackup: com.example.data.cloud.BackupData? = null/g' /app/applet/app/src/main/java/com/example/ui/viewmodel/LaborViewModel.kt

sed -i 's/repository.createDriveBackup()/repository.backupToCloud()/g' /app/applet/app/src/main/java/com/example/ui/viewmodel/LaborViewModel.kt

sed -i 's/repository.restoreFromDriveAndCloud/repository.restoreFromCloud/g' /app/applet/app/src/main/java/com/example/ui/viewmodel/LaborViewModel.kt

sed -i 's/backupNow/backupToCloudNow/g' /app/applet/app/src/main/java/com/example/ui/viewmodel/LaborViewModel.kt
sed -i 's/logoutWithDriveBackup/logoutWithCloudBackup/g' /app/applet/app/src/main/java/com/example/ui/viewmodel/LaborViewModel.kt
sed -i 's/updateDriveBackupInfo/updateCloudBackupInfo/g' /app/applet/app/src/main/java/com/example/ui/viewmodel/LaborViewModel.kt
sed -i 's/Google Drive/Cloud/g' /app/applet/app/src/main/java/com/example/ui/viewmodel/LaborViewModel.kt

