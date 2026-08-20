package com.example.data.remote

import com.example.domain.model.CashTransaction
import com.example.domain.model.LaborWorker
import com.example.domain.model.UserProfile
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
