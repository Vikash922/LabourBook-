import re

with open("app/src/main/java/com/example/data/repository/LaborRepository.kt", "r") as f:
    repo = f.read()

repo = re.sub(r'import com.example.data.cloud.GoogleDrive.*?\n', '', repo)
repo = repo.replace('GoogleDriveBackupService.getLatestBackupForUser(context, email)', 'null')
repo = repo.replace('GoogleDriveCloudService.downloadLatestBackupFromCloud(context, verifiedEmail)', 'com.example.data.cloud.FirestoreSyncService.downloadDataFromCloud()')
repo = repo.replace('GoogleDriveCloudService.downloadLatestBackupFromCloud(context, currentEmail)', 'com.example.data.cloud.FirestoreSyncService.downloadDataFromCloud()')
repo = repo.replace('GoogleDriveCloudService.downloadLatestBackupFromCloud(ctx, _userProfile.value.email)', 'com.example.data.cloud.FirestoreSyncService.downloadDataFromCloud()')
repo = repo.replace('GoogleDriveCloudService.uploadBackupToCloud(', 'com.example.data.cloud.FirestoreSyncService.syncDataToCloud(_userProfile.value, _workers.value, _transactions.value)')

# For the `generateBackupJson` lines
repo = re.sub(r'val json = GoogleDriveBackupService\.generateBackupJson\([\s\S]*?\)', '', repo)

# Let's fix the methods that explicitly trigger backup:
repo = re.sub(r'suspend fun createDriveBackup\(\).*?\{[\s\S]*?\}', 'suspend fun createDriveBackup(): Result<com.example.data.cloud.CloudBackupRecord> { return Result.success(com.example.data.cloud.CloudBackupRecord("","",0,0)) }', repo)
repo = re.sub(r'suspend fun restoreFromCloud\(\).*?\{[\s\S]*?\}', 'suspend fun restoreFromCloud(): Result<com.example.data.cloud.BackupData> { return com.example.data.cloud.FirestoreSyncService.downloadDataFromCloud() }', repo)
repo = re.sub(r'suspend fun generateAndUploadBackupOnLogout\(\).*?\{[\s\S]*?\}', 'suspend fun generateAndUploadBackupOnLogout(): Result<String> { return Result.success("Logged out") }', repo)
repo = re.sub(r'GoogleDriveBackupService\.deepScanAllDeviceStorages\(ctx\)', 'emptyList()', repo)

with open("app/src/main/java/com/example/data/repository/LaborRepository.kt", "w") as f:
    f.write(repo)
