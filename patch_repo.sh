#!/bin/bash
sed -i 's/lastDriveTime/lastCloudTime/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/lastDriveFile/lastCloudFile/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/lastDriveBackupTime/lastCloudBackupTime/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/lastDriveBackupFile/lastCloudBackupFile/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/last_drive_time/last_cloud_time/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/last_drive_file/last_cloud_file/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/createDriveBackup/backupToCloud/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/restoreFromDriveAndCloud/restoreFromCloud/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/updateDriveBackupInfo/updateCloudBackupInfo/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/Google Drive & Cloud/Cloud/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/Google Drive & Cloud Firestore/Cloud Firestore/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/Google Drive Cloud/Cloud/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/Google Drive/Cloud/g' /app/applet/app/src/main/java/com/example/data/repository/LaborRepository.kt
