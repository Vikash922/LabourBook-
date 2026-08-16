package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cloud.BackupMetadata
import com.example.ui.theme.LaborBlue
import com.example.ui.theme.LaborDivider
import com.example.ui.theme.LaborSuccess
import com.example.ui.theme.LaborTextPrimary
import com.example.ui.theme.LaborTextSecondary
import com.example.ui.viewmodel.LaborViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val lastBackupStatus by viewModel.lastBackupStatus.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    val lang = userProfile.language

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDriveBackupSheet by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    // File picker launcher to import from Google Drive / Files
    val driveFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreFromGoogleDriveUri(context, uri) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                if (success) {
                    showDriveBackupSheet = false
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF3F4F6),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = AppStrings.get("settings", lang),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    // Language button 'अ A'
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, LaborDivider),
                        modifier = Modifier
                            .clickable { showLanguageDialog = true }
                            .testTag("topbar_language_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "अ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "A",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborBlue
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Section 1: Profile Info
            item {
                Text(
                    text = AppStrings.get("profile_info", lang),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaborBlue,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Business Name Row
                        SettingsSimpleRow(
                            title = AppStrings.get("business_name", lang),
                            value = userProfile.businessName,
                            icon = Icons.Default.Person,
                            onClick = {
                                tempName = userProfile.businessName
                                showEditNameDialog = true
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        // Account / Email Row
                        SettingsSimpleRow(
                            title = AppStrings.get("account_email", lang),
                            value = userProfile.email,
                            icon = Icons.Default.VerifiedUser,
                            onClick = {
                                Toast.makeText(context, "Logged in as ${userProfile.email} (${userProfile.authProvider})", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        // Mobile Row
                        SettingsSimpleRow(
                            title = AppStrings.get("mobile", lang),
                            value = userProfile.mobile,
                            onClick = {
                                Toast.makeText(context, "Registered Mobile: ${userProfile.mobile}", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        // Language Selection Row
                        SettingsSimpleRow(
                            title = AppStrings.get("select_language", lang),
                            value = userProfile.language,
                            icon = Icons.Default.Language,
                            onClick = {
                                showLanguageDialog = true
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        // App Lock Row with Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(AppStrings.get("app_lock", lang), fontSize = 15.sp, color = Color.Black)
                            }
                            Switch(
                                checked = userProfile.appLockEnabled,
                                onCheckedChange = { viewModel.toggleAppLock() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = LaborBlue
                                )
                            )
                        }
                    }
                }
            }

            // Section 2: General & Google Drive Backup
            item {
                Text(
                    text = "Google Drive Cloud Backup",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaborBlue,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // Live Google Drive Cloud Status & Fast Action Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = LaborBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Google Account Cloud",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = userProfile.email.ifBlank { "jyoti3322114455@gmail.com" },
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            if (isCloudSyncing || isBackingUp || isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = LaborBlue
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Banner
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = if (lastBackupStatus.contains("failed", ignoreCase = true) || lastBackupStatus.contains("No cloud backup", ignoreCase = true)) {
                                Color(0xFFFEF2F2)
                            } else if (lastBackupStatus.contains("successful", ignoreCase = true) || lastBackupStatus.contains("Restored", ignoreCase = true)) {
                                Color(0xFFF0FDF4)
                            } else {
                                Color(0xFFEFF6FF)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (lastBackupStatus.contains("failed", ignoreCase = true) || lastBackupStatus.contains("No cloud backup", ignoreCase = true)) Color(0xFFFECACA)
                                else if (lastBackupStatus.contains("successful", ignoreCase = true) || lastBackupStatus.contains("Restored", ignoreCase = true)) Color(0xFFBBF7D0)
                                else Color(0xFFBFDBFE)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (lastBackupStatus.contains("failed", ignoreCase = true)) Icons.Default.Policy
                                    else if (lastBackupStatus.contains("successful", ignoreCase = true) || lastBackupStatus.contains("Restored", ignoreCase = true)) Icons.Default.CloudDone
                                    else Icons.Default.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (lastBackupStatus.contains("failed", ignoreCase = true) || lastBackupStatus.contains("No cloud backup", ignoreCase = true)) Color(0xFFDC2626)
                                    else if (lastBackupStatus.contains("successful", ignoreCase = true) || lastBackupStatus.contains("Restored", ignoreCase = true)) Color(0xFF16A34A)
                                    else LaborBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = lastBackupStatus,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (lastBackupStatus.contains("failed", ignoreCase = true) || lastBackupStatus.contains("No cloud backup", ignoreCase = true)) Color(0xFF991B1B)
                                    else if (lastBackupStatus.contains("successful", ignoreCase = true) || lastBackupStatus.contains("Restored", ignoreCase = true)) Color(0xFF166534)
                                    else Color(0xFF1E40AF)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fast Testing Action Buttons: "Backup Now" and "Restore from Cloud"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    isBackingUp = true
                                    viewModel.backupNow { success, msg ->
                                        isBackingUp = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = !isBackingUp && !isRestoring,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("drive_backup_now_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBackingUp) "Backing up..." else "Backup Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = {
                                    isRestoring = true
                                    viewModel.restoreFromCloudNow { success, msg ->
                                        isRestoring = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = !isBackingUp && !isRestoring,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("drive_restore_cloud_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, LaborBlue),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LaborBlue)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = LaborBlue)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isRestoring) "Restoring..." else "Restore Cloud", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LaborBlue)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Text(
                    text = AppStrings.get("general", lang),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaborBlue,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Google Drive Backup & Sync Row
                        SettingsSimpleRow(
                            title = AppStrings.get("drive_backup", lang),
                            value = if (userProfile.lastDriveBackupTime != "Never") "Synced" else "Setup",
                            icon = Icons.Default.CloudSync,
                            onClick = {
                                showDriveBackupSheet = true
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        // Full App CSV Backup Export
                        SettingsSimpleRow(
                            title = "Export All Data (.CSV - Low Storage)",
                            value = "Share/Save",
                            icon = Icons.Default.TableChart,
                            onClick = {
                                viewModel.exportAndShareBackupCsv(context)
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        // Import / Restore from File (.CSV / .JSON)
                        SettingsSimpleRow(
                            title = "Import & Restore Data File",
                            value = ".CSV / .JSON",
                            icon = Icons.Default.FileDownload,
                            onClick = {
                                driveFilePicker.launch("*/*")
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        // Consolidated Reports Hub
                        SettingsSimpleRow(
                            title = "Consolidated Reports & PDF Hub",
                            icon = Icons.Default.PictureAsPdf,
                            onClick = {
                                viewModel.navigateTo(Screen.BatchPdfHub)
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        SettingsSimpleRow(
                            title = AppStrings.get("privacy_policy", lang),
                            icon = Icons.Default.Policy,
                            onClick = {
                                Toast.makeText(context, "Opening Privacy Policy", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        SettingsSimpleRow(
                            title = AppStrings.get("terms_conditions", lang),
                            icon = Icons.Default.PictureAsPdf,
                            onClick = {
                                Toast.makeText(context, "Opening Terms & Conditions", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        SettingsSimpleRow(
                            title = AppStrings.get("rating_feedback", lang),
                            icon = Icons.Default.StarRate,
                            onClick = {
                                Toast.makeText(context, "Thank you for rating Laborbook!", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6))

                        SettingsSimpleRow(
                            title = AppStrings.get("logout", lang),
                            icon = Icons.AutoMirrored.Filled.Logout,
                            onClick = {
                                showLogoutConfirmDialog = true
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // Version info footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Laborbook",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaborBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "v2.5.0",
                            fontSize = 14.sp,
                            color = LaborTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "100% Secure Cloud & Local Storage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }

    // Google Drive Backup & Import Bottom Sheet
    var backupRefreshKey by remember { mutableStateOf(0) }
    var backupToRestoreConfirm by remember { mutableStateOf<com.example.data.cloud.BackupMetadata?>(null) }

    if (backupToRestoreConfirm != null) {
        val target = backupToRestoreConfirm!!
        AlertDialog(
            onDismissRequest = { backupToRestoreConfirm = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = LaborBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Backup?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Do you want to restore data from this snapshot?",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = LaborTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Snapshot Time: ${target.dateString}\n• Workers to recover: ${target.workerCount}\n• Cash records: ${target.transactionCount}",
                        fontSize = 13.sp,
                        color = LaborTextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "All labour attendance histories and cash logs from this point will be restored.",
                        fontSize = 12.sp,
                        color = LaborBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = target.file
                        if (file != null) {
                            viewModel.restoreFromLocalBackup(file) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                backupRefreshKey++
                                backupToRestoreConfirm = null
                                if (success) showDriveBackupSheet = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                ) {
                    Text("Confirm & Restore", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestoreConfirm = null }) {
                    Text("Cancel", color = LaborTextSecondary)
                }
            }
        )
    }

    if (showDriveBackupSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val availableBackups = remember(showDriveBackupSheet, backupRefreshKey) { 
            viewModel.getGoogleDriveBackups(context) 
        }

        ModalBottomSheet(
            onDismissRequest = { showDriveBackupSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F0FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = LaborBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Google Drive Backup & Restore",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborTextPrimary
                            )
                            Text(
                                text = userProfile.email,
                                fontSize = 12.sp,
                                color = LaborTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detail Guarantee Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = LaborBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100% Comprehensive Cloud Protection",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• All labour names, wages, full calendar attendance logs & cash records are saved.\n• Restore anytime if you delete workers or switch devices.",
                            fontSize = 11.sp,
                            color = Color(0xFF1E3A8A),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // PRIMARY ACTIONS ROW: 1) Save Backup, 2) Quick Restore
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Take Backup Card
                    Card(
                        modifier = Modifier
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Take Backup", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF15803D))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Save current state to Drive", fontSize = 10.sp, color = Color(0xFF166534), maxLines = 1)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    isBackingUp = true
                                    viewModel.backupToGoogleDrive(context) { success, msg ->
                                        isBackingUp = false
                                        backupRefreshKey++
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = !isBackingUp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text(if (isBackingUp) "Saving..." else "Backup Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // 2. Restore Backup Card
                    Card(
                        modifier = Modifier
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Restore, contentDescription = null, tint = LaborBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore Data", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LaborBlue)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Recover deleted labour", fontSize = 10.sp, color = Color(0xFF1E40AF), maxLines = 1)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    isRestoring = true
                                    viewModel.restoreFromCloudNow { success, msg ->
                                        isRestoring = false
                                        backupRefreshKey++
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (success) showDriveBackupSheet = false
                                    }
                                },
                                enabled = !isRestoring,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LaborBlue),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text(if (isRestoring) "Restoring..." else "Restore Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Row: Share CSV / Import File
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.exportAndShareBackupCsv(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(19.dp),
                        border = BorderStroke(1.dp, Color(0xFF059669)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF059669))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share .CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }

                    OutlinedButton(
                        onClick = { driveFilePicker.launch("*/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(19.dp),
                        border = BorderStroke(1.dp, LaborBlue),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = LaborBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import File", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LaborBlue)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Previous Snapshots Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Available Snapshots (${availableBackups.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LaborTextPrimary
                    )
                    Text(
                        text = "Tap Restore to load",
                        fontSize = 11.sp,
                        color = LaborTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (availableBackups.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF9FAFB),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Text(
                            text = "No previous snapshots saved yet. Tap 'Backup Now' to create your first cloud snapshot point.",
                            fontSize = 12.sp,
                            color = LaborTextSecondary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableBackups) { backup ->
                            val isSafety = backup.fileName.startsWith("safety_")
                            val isCsv = backup.fileName.endsWith(".csv")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSafety) Color(0xFFFFFBEB) else Color(0xFFF9FAFB)
                                ),
                                border = BorderStroke(1.dp, if (isSafety) Color(0xFFFDE68A) else Color(0xFFE5E7EB))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = backup.dateString,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LaborTextPrimary
                                            )
                                            if (isSafety) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFFEF3C7)
                                                ) {
                                                    Text(
                                                        text = "Pre-Delete",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF92400E),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "👥 ${backup.workerCount} Workers • 💵 ${backup.transactionCount} Cash (${backup.fileSizeKb} KB)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (backup.workerCount > 0) Color(0xFF15803D) else LaborTextSecondary
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            backupToRestoreConfirm = backup
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LaborBlue),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    var isLoggingOut by remember { mutableStateOf(false) }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoggingOut) showLogoutConfirmDialog = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = LaborBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.get("logout", lang), fontWeight = FontWeight.Bold)
                }
            },
            text = { 
                Column {
                    Text(
                        text = "Are you sure you want to log out of Laborbook?",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = LaborTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your records will be automatically backed up to your Google Drive (${userProfile.email}) before logging out. When you sign in again with this Google account, all your data will be automatically restored.",
                        fontSize = 12.sp,
                        color = LaborTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoggingOut = true
                        viewModel.logoutWithDriveBackup { success, msg ->
                            isLoggingOut = false
                            showLogoutConfirmDialog = false
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isLoggingOut,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backing up & Logging out...", color = Color.White, fontSize = 12.sp)
                    } else {
                        Text("Backup & Logout", color = Color.White)
                    }
                }
            },
            dismissButton = {
                if (!isLoggingOut) {
                    TextButton(onClick = { showLogoutConfirmDialog = false }) {
                        Text(AppStrings.get("cancel", lang))
                    }
                }
            }
        )
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        val languages = listOf("English", "Hindi (हिंदी)", "Marathi (मराठी)", "Gujarati (ગુજરાતી)", "Bengali (বাংলা)", "Tamil (தமிழ்)")
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Translate, contentDescription = null, tint = LaborBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.get("select_language", lang), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    languages.forEach { languageName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(languageName)
                                    Toast.makeText(context, "Language changed to $languageName", Toast.LENGTH_SHORT).show()
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = languageName,
                                fontSize = 15.sp,
                                fontWeight = if (userProfile.language == languageName) FontWeight.Bold else FontWeight.Normal,
                                color = if (userProfile.language == languageName) LaborBlue else LaborTextPrimary
                            )
                            if (userProfile.language == languageName) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "Selected",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LaborBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(AppStrings.get("ok", lang))
                }
            }
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(AppStrings.get("business_name", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBusinessName(tempName)
                        showEditNameDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                ) {
                    Text(AppStrings.get("save", lang), color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun SettingsSimpleRow(
    title: String,
    value: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(title, fontSize = 15.sp, color = Color.Black)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(value, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
