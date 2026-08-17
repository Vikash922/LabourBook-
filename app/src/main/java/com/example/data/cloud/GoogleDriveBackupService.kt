package com.example.data.cloud

import android.content.Context
import android.net.Uri

object GoogleDriveBackupService {
    fun getAvailableBackupsForUser(context: Context, email: String): List<BackupMetadata> {
        return emptyList()
    }

    fun getLatestBackupForUser(context: Context, email: String): BackupData? {
        return null
    }

    suspend fun readBackupFromUri(context: Context, uri: Uri): Result<BackupData> {
        return Result.failure(Exception("Not implemented"))
    }

    fun parseBackupUniversal(content: String): Result<BackupData> {
        return Result.failure(Exception("Not implemented"))
    }
}
