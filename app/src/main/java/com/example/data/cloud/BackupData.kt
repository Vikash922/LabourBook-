package com.example.data.cloud

import com.example.data.model.CashTransaction
import com.example.data.model.LaborWorker
import com.example.data.model.UserProfile
import java.io.File

data class BackupData(
    val userProfile: UserProfile? = null,
    val workers: List<LaborWorker> = emptyList(),
    val transactions: List<CashTransaction> = emptyList(),
    val totalWorkers: Int = 0,
    val totalTransactions: Int = 0,
    val backupTimestamp: String = "",
    val driveFileId: String? = null,
    val accountEmail: String = ""
)

data class BackupMetadata(
    val fileName: String,
    val dateString: String,
    val fileSizeKb: Double,
    val workerCount: Int,
    val transactionCount: Int,
    val accountEmail: String,
    val file: File? = null
)

data class CloudBackupRecord(
    val backupTimestamp: String = "",
    val driveFileId: String = "",
    val totalWorkers: Int = 0,
    val totalTransactions: Int = 0
)
