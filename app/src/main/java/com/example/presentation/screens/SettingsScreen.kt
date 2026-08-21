package com.example.presentation.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.AppStrings
import com.example.presentation.theme.LaborBlue
import com.example.presentation.theme.LaborTextPrimary
import com.example.presentation.theme.LaborTextSecondary
import com.example.presentation.viewmodel.LaborViewModel
import com.example.presentation.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val lang = userProfile.language

    var showCloudBackupSheet by remember { mutableStateOf(false) }
    var showCsvOptionsSheet by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var showEditMobileDialog by remember { mutableStateOf(false) }
    var tempMobile by remember { mutableStateOf("") }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AppStrings.get("settings", lang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.LaborHome) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Compact User Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userProfile.businessName.take(1).uppercase().ifBlank { "L" },
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userProfile.businessName.ifBlank { "My Business" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = LaborBlue,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            tempName = userProfile.businessName
                                            showEditNameDialog = true
                                        }
                                )
                            }
                            Text(
                                text = userProfile.email.ifBlank { "Google Account" },
                                fontSize = 11.5.sp,
                                color = LaborTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (userProfile.mobile.isNotBlank()) {
                                Text(
                                    text = "📞 ${userProfile.mobile}",
                                    fontSize = 11.5.sp,
                                    color = LaborTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Compact Group: Cloud Sync & Backup
            item {
                CompactSettingsGroup(title = "CLOUD SYNC & BACKUP") {
                    CompactSettingsRow(
                        icon = Icons.Default.CloudDone,
                        iconTint = Color(0xFF10B981),
                        title = "Cloud Backup & Sync",
                        subtitle = if (isCloudSyncing || isBackingUp || isRestoring) "Syncing with cloud..." else "Auto-synced & Protected",
                        showLoading = isCloudSyncing || isBackingUp || isRestoring,
                        onClick = { showCloudBackupSheet = true }
                    )
                }
            }

            // 3. Compact Group: Account Details
            item {
                CompactSettingsGroup(title = "ACCOUNT & BUSINESS") {
                    CompactSettingsRow(
                        icon = Icons.Default.Business,
                        iconTint = LaborBlue,
                        title = AppStrings.get("business_name", lang),
                        subtitle = userProfile.businessName.ifBlank { "Set Business Name" },
                        onClick = {
                            tempName = userProfile.businessName
                            showEditNameDialog = true
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    CompactSettingsRow(
                        icon = Icons.Default.Call,
                        iconTint = LaborBlue,
                        title = AppStrings.get("mobile", lang),
                        subtitle = userProfile.mobile.ifBlank { "Add Mobile Number" },
                        onClick = {
                            tempMobile = userProfile.mobile
                            showEditMobileDialog = true
                        }
                    )
                }
            }

            // 4. Compact Group: Reports & Export
            item {
                CompactSettingsGroup(title = "DATA & REPORTS") {
                    CompactSettingsRow(
                        icon = Icons.Default.PictureAsPdf,
                        iconTint = Color(0xFFDC2626),
                        title = "Reports & PDF Hub",
                        subtitle = "Generate registers & summary PDFs",
                        onClick = { viewModel.navigateTo(Screen.BatchPdfHub) }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    CompactSettingsRow(
                        icon = Icons.Default.TableChart,
                        iconTint = Color(0xFF059669),
                        title = "Export Data (.CSV)",
                        subtitle = "Save or share spreadsheet backup",
                        onClick = { showCsvOptionsSheet = true }
                    )
                }
            }

            // 5. Compact Group: Support & Legal
            item {
                CompactSettingsGroup(title = "SUPPORT & LEGAL") {
                    CompactSettingsRow(
                        icon = Icons.Default.Policy,
                        iconTint = Color(0xFF64748B),
                        title = AppStrings.get("privacy_policy", lang),
                        onClick = { viewModel.showMessage("Opening Privacy Policy") }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    CompactSettingsRow(
                        icon = Icons.Default.StarRate,
                        iconTint = Color(0xFFF59E0B),
                        title = AppStrings.get("rating_feedback", lang),
                        onClick = { viewModel.showMessage("Thank you for rating Laborbook!") }
                    )
                }
            }

            // 6. Compact Logout Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    CompactSettingsRow(
                        icon = Icons.Default.Logout,
                        iconTint = Color(0xFFDC2626),
                        title = AppStrings.get("logout", lang),
                        subtitle = "Auto-backs up before sign out",
                        titleColor = Color(0xFFDC2626),
                        onClick = { showLogoutConfirmDialog = true }
                    )
                }
            }

            // 7. Compact Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Laborbook v2.5.0 • Offline-First & Cloud Sync",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF5F3FF),
                        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "App Made by Vikash",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9)
                            )
                        }
                    }
                }
            }
        }
    }

    // Cloud Backup Sheet
    if (showCloudBackupSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCloudBackupSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                            text = "Cloud Backup & Restore",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaborTextPrimary
                        )
                        Text(
                            text = userProfile.email.ifBlank { "Google Account" },
                            fontSize = 11.5.sp,
                            color = LaborTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                CompactSettingsGroup(title = "ACTIONS") {
                    CompactSettingsRow(
                        icon = Icons.Default.CloudUpload,
                        iconTint = Color(0xFF10B981),
                        title = if (isBackingUp) "Saving..." else "Backup to Cloud",
                        subtitle = "Save current state to Firebase",
                        onClick = {
                            if (!isBackingUp) {
                                isBackingUp = true
                                viewModel.backupToCloudNow { success, msg ->
                                    isBackingUp = false
                                    viewModel.showMessage(msg)
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    CompactSettingsRow(
                        icon = Icons.Default.CloudDownload,
                        iconTint = LaborBlue,
                        title = if (isRestoring) "Restoring..." else "Restore from Cloud",
                        subtitle = "Load previous state from Firebase",
                        onClick = {
                            if (!isRestoring) {
                                isRestoring = true
                                viewModel.restoreFromCloudNow { success, msg ->
                                    isRestoring = false
                                    viewModel.showMessage(msg)
                                    if (success) showCloudBackupSheet = false
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        var isLoggingOut by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isLoggingOut) showLogoutConfirmDialog = false },
            title = {
                Text(AppStrings.get("logout", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    text = "Your records will be automatically saved to Cloud before sign out.",
                    fontSize = 13.sp,
                    color = LaborTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoggingOut = true
                        viewModel.logoutWithCloudBackup { success, msg ->
                            isLoggingOut = false
                            showLogoutConfirmDialog = false
                        }
                    },
                    enabled = !isLoggingOut,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Logout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            },
            dismissButton = {
                if (!isLoggingOut) {
                    TextButton(onClick = { showLogoutConfirmDialog = false }) {
                        Text(AppStrings.get("cancel", lang), color = LaborTextSecondary, fontSize = 13.sp)
                    }
                }
            }
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(AppStrings.get("business_name", lang), fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
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
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                ) {
                    Text(AppStrings.get("save", lang), color = Color.White, fontSize = 13.sp)
                }
            }
        )
    }

    if (showEditMobileDialog) {
        AlertDialog(
            onDismissRequest = { showEditMobileDialog = false },
            title = { Text(AppStrings.get("mobile", lang), fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                OutlinedTextField(
                    value = tempMobile,
                    onValueChange = { tempMobile = it },
                    placeholder = { Text("Enter Mobile Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateMobile(tempMobile.trim())
                        showEditMobileDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                ) {
                    Text(AppStrings.get("save", lang), color = Color.White, fontSize = 13.sp)
                }
            }
        )
    }

    // CSV Backup Options Bottom Sheet
    if (showCsvOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCsvOptionsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Full App CSV Backup",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaborTextPrimary
                )
                Text(
                    text = "Export worker attendance & cash records",
                    fontSize = 11.5.sp,
                    color = LaborTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.saveCsvBackupToDevice(context) { success, msg ->
                            viewModel.showMessage(msg)
                            if (success) showCsvOptionsSheet = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Save to Device", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.exportAndShareBackupCsv(context) { success, msg ->
                            if (!success) viewModel.showMessage(msg)
                        }
                        showCsvOptionsSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF059669))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Share CSV File", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF059669))
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    CompactSettingsGroup(title = title, content = content)
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = LaborBlue,
    titleColor: Color = LaborTextPrimary,
    onClick: () -> Unit
) {
    CompactSettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        iconTint = iconTint,
        titleColor = titleColor,
        onClick = onClick
    )
}

@Composable
fun CompactSettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun CompactSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = LaborBlue,
    titleColor: Color = LaborTextPrimary,
    showLoading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = LaborTextSecondary
                    )
                }
            }
        }
        if (showLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = LaborBlue
            )
        } else {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
