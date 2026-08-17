sed -i 's/import com.example.data.cloud.GoogleDrive.*//g' app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/GoogleDriveBackupService.getLatestBackupForUser(context, email)/null/g' app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/GoogleDriveBackupService.getLatestBackupForUser(context, verifiedEmail)/null/g' app/src/main/java/com/example/data/repository/LaborRepository.kt

sed -i 's/GoogleDriveCloudService.downloadLatestBackupFromCloud(context, email)/Result.failure<BackupData>(Exception("removed"))/g' app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/GoogleDriveCloudService.downloadLatestBackupFromCloud(context, currentEmail)/Result.failure<BackupData>(Exception("removed"))/g' app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/GoogleDriveCloudService.downloadLatestBackupFromCloud(context, verifiedEmail)/Result.failure<BackupData>(Exception("removed"))/g' app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/GoogleDriveCloudService.downloadLatestBackupFromCloud(ctx, _userProfile.value.email)/Result.failure<BackupData>(Exception("removed"))/g' app/src/main/java/com/example/data/repository/LaborRepository.kt

sed -i 's/GoogleDriveCloudService.uploadBackupToCloud/com.example.data.cloud.FirestoreSyncService.syncDataToCloud/g' app/src/main/java/com/example/data/repository/LaborRepository.kt
sed -i 's/val json = GoogleDriveBackupService.generateBackupJson.*//g' app/src/main/java/com/example/data/repository/LaborRepository.kt

# For deepScanAllDeviceStorages
sed -i 's/GoogleDriveBackupService.deepScanAllDeviceStorages(ctx)/emptyList()/g' app/src/main/java/com/example/data/repository/LaborRepository.kt

# Replace createDriveBackup return type and body if needed
# We can just let it fail if we don't fix it properly, but let's see.

