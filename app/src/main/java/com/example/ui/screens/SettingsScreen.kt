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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
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
    var showCloudBackupSheet by remember { mutableStateOf(false) }
    var showCsvOptionsSheet by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var showEditMobileDialog by remember { mutableStateOf(false) }
    var tempMobile by remember { mutableStateOf("") }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    // File picker launcher to import from Cloud / Files

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = AppStrings.get("settings", lang),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = LaborTextPrimary
                    )

                    // Language button 'अ A'
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
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
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "A",
                                fontSize = 13.sp,
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Card Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Surface(
                            shape = CircleShape,
                            color = LaborBlue,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userProfile.businessName.take(1).uppercase().ifBlank { "L" },
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = userProfile.businessName.ifBlank { "My Business" },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit name",
                                    tint = LaborBlue,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            tempName = userProfile.businessName
                                            showEditNameDialog = true
                                        }
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = userProfile.email.ifBlank { "No email connected" },
                                fontSize = 12.sp,
                                color = LaborTextSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )

                            if (userProfile.mobile.isNotBlank()) {
                                Text(
                                    text = "📞 ${userProfile.mobile}",
                                    fontSize = 12.sp,
                                    color = LaborTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Cloud Cloud Sync Card
                        // Section: Cloud Backup & Security (Minimal UI)
            item {
                SettingsGroupCard(title = "Cloud Backup & Security") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCloudBackupSheet = true }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFECFDF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Manage Cloud Backup",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LaborTextPrimary
                                )
                                Text(
                                    text = if (isCloudSyncing || isBackingUp || isRestoring) "Syncing with cloud..." else "Auto-synced & Protected",
                                    fontSize = 11.sp,
                                    color = LaborTextSecondary
                                )
                            }
                        }
                        if (isCloudSyncing || isBackingUp || isRestoring) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = LaborBlue
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Section: Account & Profile Settings
            item {
                SettingsGroupCard(title = AppStrings.get("profile_info", lang)) {
                    SettingsRowItem(
                        icon = Icons.Default.Business,
                        title = AppStrings.get("business_name", lang),
                        subtitle = userProfile.businessName.ifBlank { "Tap to set" },
                        onClick = {
                            tempName = userProfile.businessName
                            showEditNameDialog = true
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    SettingsRowItem(
                        icon = Icons.Default.Call,
                        title = AppStrings.get("mobile", lang),
                        subtitle = userProfile.mobile.ifBlank { "Tap to add number" },
                        onClick = {
                            tempMobile = userProfile.mobile
                            showEditMobileDialog = true
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    SettingsRowItem(
                        icon = Icons.Default.Language,
                        title = AppStrings.get("select_language", lang),
                        subtitle = userProfile.language,
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            // Section: Data, Reports & Export
            item {
                SettingsGroupCard(title = "Data & Reports") {
                    SettingsRowItem(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Reports & PDF Hub",
                        subtitle = "Generate consolidated worker & cash registers",
                        iconTint = Color(0xFFDC2626),
                        onClick = { viewModel.navigateTo(Screen.BatchPdfHub) }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    SettingsRowItem(
                        icon = Icons.Default.TableChart,
                        title = "Export Data (.CSV)",
                        subtitle = "Save or share full backup as .CSV spreadsheet",
                        iconTint = Color(0xFF059669),
                        onClick = { showCsvOptionsSheet = true }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    
                }
            }

            // Section: About & Support
            item {
                SettingsGroupCard(title = "Support & Legal") {
                    SettingsRowItem(
                        icon = Icons.Default.Policy,
                        title = AppStrings.get("privacy_policy", lang),
                        onClick = { viewModel.showMessage("Opening Privacy Policy") }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    SettingsRowItem(
                        icon = Icons.Default.PictureAsPdf,
                        title = AppStrings.get("terms_conditions", lang),
                        onClick = { viewModel.showMessage("Opening Terms & Conditions") }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    SettingsRowItem(
                        icon = Icons.Default.StarRate,
                        title = AppStrings.get("rating_feedback", lang),
                        iconTint = Color(0xFFF59E0B),
                        onClick = { viewModel.showMessage("Thank you for rating Laborbook!") }
                    )
                }
            }

            // Section: Logout
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    SettingsRowItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = AppStrings.get("logout", lang),
                        subtitle = "Auto-backs up records before signing out",
                        iconTint = Color(0xFFDC2626),
                        titleColor = Color(0xFFDC2626),
                        onClick = { showLogoutConfirmDialog = true }
                    )
                }
            }

            // Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Laborbook v2.5.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LaborTextSecondary
                    )
                    Text(
                        text = "100% Offline-First & Cloud Backup",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFF5F3FF),
                        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "App Made by Vikash",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF6D28D9),
                                style = androidx.compose.ui.text.TextStyle(
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Cloud Backup & Restore Bottom Sheet
    var backupRefreshKey by remember { mutableStateOf(0) }
    

    if (showCloudBackupSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showCloudBackupSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Sheet Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = LaborBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Cloud Backup & Restore",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborTextPrimary
                            )
                            Text(
                                text = userProfile.email.ifBlank { "Google Account" },
                                fontSize = 12.sp,
                                color = LaborTextSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action List
                SettingsGroupCard(title = "Cloud Actions") {
                    SettingsRowItem(
                        icon = Icons.Default.CloudUpload,
                        title = if (isBackingUp) "Saving..." else "Backup to Cloud",
                        subtitle = "Save current state to Firebase",
                        iconTint = Color(0xFF10B981),
                        onClick = {
                            if (!isBackingUp) {
                                isBackingUp = true
                                viewModel.backupToCloudNow() { success, msg ->
                                    isBackingUp = false
                                    backupRefreshKey++
                                    viewModel.showMessage(msg)
                                }
                            }
                        }
                    )
                    
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    SettingsRowItem(
                        icon = Icons.Default.CloudDownload,
                        title = if (isRestoring) "Restoring..." else "Restore from Cloud",
                        subtitle = "Load previous state from Firebase",
                        iconTint = LaborBlue,
                        onClick = {
                            if (!isRestoring) {
                                isRestoring = true
                                viewModel.restoreFromCloudNow { success, msg ->
                                    isRestoring = false
                                    backupRefreshKey++
                                    viewModel.showMessage(msg)
                                    if (success) showCloudBackupSheet = false
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(32.dp))
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
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.get("logout", lang), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = { 
                Column {
                    Text(
                        text = "Are you sure you want to log out?",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = LaborTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your records will be automatically saved to Cloud (${userProfile.email}) before sign out.",
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
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Logging out...", color = Color.White, fontSize = 12.sp)
                    } else {
                        Text("Save & Logout", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isLoggingOut) {
                    TextButton(onClick = { showLogoutConfirmDialog = false }) {
                        Text(AppStrings.get("cancel", lang), color = LaborTextSecondary)
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
                    Text(AppStrings.get("select_language", lang), fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                                    viewModel.showMessage("Language changed to $languageName")
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = languageName,
                                fontSize = 14.sp,
                                fontWeight = if (userProfile.language == languageName) FontWeight.Bold else FontWeight.Normal,
                                color = if (userProfile.language == languageName) LaborBlue else LaborTextPrimary
                            )
                            if (userProfile.language == languageName) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LaborBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
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
                    shape = RoundedCornerShape(10.dp),
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
                    Text(AppStrings.get("save", lang), color = Color.White)
                }
            }
        )
    }

    if (showEditMobileDialog) {
        AlertDialog(
            onDismissRequest = { showEditMobileDialog = false },
            title = { Text(AppStrings.get("mobile", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = tempMobile,
                    onValueChange = { tempMobile = it },
                    placeholder = { Text("Enter Mobile Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
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
                    Text(AppStrings.get("save", lang), color = Color.White)
                }
            }
        )
    }

    // CSV Backup & Export Options Bottom Sheet
    if (showCsvOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCsvOptionsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Full App CSV Backup",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaborTextPrimary
                        )
                        Text(
                            text = "Export all worker attendance & cash records",
                            fontSize = 12.sp,
                            color = LaborTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Option 1: Direct Save to Device (Downloads)
                Button(
                    onClick = {
                        viewModel.saveCsvBackupToDevice(context) { success, msg ->
                            viewModel.showMessage(msg)
                            if (success) showCsvOptionsSheet = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("save_csv_device_direct_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save to Device (Downloads)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Share / Send CSV File
                OutlinedButton(
                    onClick = {
                        viewModel.exportAndShareBackupCsv(context) { success, msg ->
                            if (!success) {
                                viewModel.showMessage(msg)
                            }
                        }
                        showCsvOptionsSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("share_csv_file_btn"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF059669))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share .CSV via WhatsApp / Email",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF059669)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = LaborTextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
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
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
        )
    }
}
