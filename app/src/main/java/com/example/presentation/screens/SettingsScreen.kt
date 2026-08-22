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
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    var showCsvOptionsSheet by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var showEditMobileDialog by remember { mutableStateOf(false) }
    var tempMobile by remember { mutableStateOf("") }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.LaborHome) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    LanguageSwitchPill(
                        lang = lang,
                        onToggle = {
                            viewModel.setLanguage(if (lang == "hi") "en" else "hi")
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. User Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LaborBlue,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userProfile.businessName.take(1).uppercase().ifBlank { "L" },
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    tempName = userProfile.businessName
                                    showEditNameDialog = true
                                }
                            ) {
                                Text(
                                    text = userProfile.businessName.ifBlank { "My Business" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Business Name",
                                    tint = LaborBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = userProfile.email.ifBlank { "Google Account" },
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (userProfile.mobile.isNotBlank()) {
                                Text(
                                    text = "📞 ${userProfile.mobile}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Business Details & Preferences
            item {
                CompactSettingsGroup(title = "BUSINESS DETAILS") {
                    // Business Name Edit
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

                    // Mobile Number Edit
                    CompactSettingsRow(
                        icon = Icons.Default.Call,
                        iconTint = Color(0xFF0284C7),
                        title = AppStrings.get("mobile", lang),
                        subtitle = userProfile.mobile.ifBlank { "Add Mobile Number" },
                        onClick = {
                            tempMobile = userProfile.mobile
                            showEditMobileDialog = true
                        }
                    )
                }
            }

            // 3. Cloud Sync & Backup Section (Simple & Direct)
            item {
                CompactSettingsGroup(title = "CLOUD BACKUP & RECOVERY") {
                    CompactSettingsRow(
                        icon = Icons.Default.CloudUpload,
                        iconTint = Color(0xFF10B981),
                        title = if (isBackingUp) "Saving to Cloud..." else "Backup to Cloud Now",
                        subtitle = "Safe copy of all workers & cashbook",
                        showLoading = isBackingUp,
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
                        iconTint = Color(0xFF3B82F6),
                        title = if (isRestoring) "Restoring Data..." else "Restore from Cloud",
                        subtitle = "Recover saved records to device",
                        showLoading = isRestoring,
                        onClick = {
                            if (!isRestoring) {
                                isRestoring = true
                                viewModel.restoreFromCloudNow { success, msg ->
                                    isRestoring = false
                                    viewModel.showMessage(msg)
                                }
                            }
                        }
                    )
                }
            }

            // 4. Data & Reports Section
            item {
                CompactSettingsGroup(title = "REPORTS & EXPORT") {
                    CompactSettingsRow(
                        icon = Icons.Default.PictureAsPdf,
                        iconTint = Color(0xFFDC2626),
                        title = "Reports & PDF Hub",
                        subtitle = "Download attendance register & summaries",
                        onClick = { viewModel.navigateTo(Screen.BatchPdfHub) }
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    CompactSettingsRow(
                        icon = Icons.Default.TableChart,
                        iconTint = Color(0xFF059669),
                        title = "Export Data (Excel / CSV)",
                        subtitle = "Download or share spreadsheets",
                        onClick = { showCsvOptionsSheet = true }
                    )
                }
            }

            // 5. App & Support Section
            item {
                CompactSettingsGroup(title = "SUPPORT & ABOUT") {
                    CompactSettingsRow(
                        icon = Icons.Default.StarRate,
                        iconTint = Color(0xFFF59E0B),
                        title = AppStrings.get("rating_feedback", lang),
                        subtitle = "Rate Laborbook 5-stars",
                        onClick = { viewModel.showMessage("Thank you for your feedback!") }
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    CompactSettingsRow(
                        icon = Icons.Default.Policy,
                        iconTint = Color(0xFF64748B),
                        title = AppStrings.get("privacy_policy", lang),
                        subtitle = "Terms & Data Protection",
                        onClick = { showPrivacyPolicyDialog = true }
                    )
                }
            }

            // 6. Logout Button Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
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

            // 7. Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Laborbook v2.5.0 • Safe & Secure",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF5F3FF),
                        border = BorderStroke(1.dp, Color(0xFFDDD6FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "App Made by Vikash",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9)
                            )
                        }
                    }
                }
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
                    text = "Your records will be automatically backed up to Cloud before sign out.",
                    fontSize = 13.5.sp,
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

    // Edit Business Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(AppStrings.get("business_name", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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

    // Edit Mobile Dialog
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

    // Privacy Policy / Gratitude Overlay Popup Dialog
    if (showPrivacyPolicyDialog) {
        Dialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("privacy_policy_popup"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top glowing badge with red heart
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "❤️",
                                fontSize = 28.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = "Thank You for Using My App!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Subtitle / Body
                    Text(
                        text = "This is my first app, created with passion and hard work.",
                        fontSize = 13.5.sp,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Developer Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "👨‍💻",
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Created by Vikash Singh",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Thank you for supporting my journey! ❤️",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE11D48),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Got It Button
                    Button(
                        onClick = { showPrivacyPolicyDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("privacy_got_it_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LaborBlue),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Text(
                            text = "Got it",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }
                }
            }
        }
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
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Export Excel / CSV Data",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LaborTextPrimary
                )
                Text(
                    text = "Export worker attendance & cash transactions spreadsheet",
                    fontSize = 12.sp,
                    color = LaborTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.saveCsvBackupToDevice(context) { success, msg ->
                            viewModel.showMessage(msg)
                            if (success) showCsvOptionsSheet = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Save to Device", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.exportAndShareBackupCsv(context) { success, msg ->
                            if (!success) viewModel.showMessage(msg)
                        }
                        showCsvOptionsSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF059669))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Share Spreadsheet File", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF059669))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun LanguageSwitchPill(
    lang: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHindi = lang == "hi"
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, Color(0xFF1656D6)),
        modifier = modifier
            .padding(end = 12.dp)
            .clickable { onToggle() }
            .testTag("top_language_toggle_pill")
    ) {
        Row(
            modifier = Modifier.padding(2.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hindi indicator 'अ'
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isHindi) Color(0xFF1656D6) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "अ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHindi) Color.White else Color(0xFF1656D6)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // English indicator 'A'
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (!isHindi) Color(0xFF1656D6) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (!isHindi) Color.White else Color(0xFF1656D6)
                )
            }
        }
    }
}

@Composable
fun LanguagePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) LaborBlue else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isSelected) LaborBlue else Color(0xFFCBD5E1)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF475569),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
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
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
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
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = LaborTextSecondary
                    )
                }
            }
        }
        if (showLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                strokeWidth = 2.dp,
                color = LaborBlue
            )
        } else {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
