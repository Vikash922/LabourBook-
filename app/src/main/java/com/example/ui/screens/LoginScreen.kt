package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LaborBlue
import com.example.ui.theme.LaborSuccess
import com.example.ui.theme.LaborTextPrimary
import com.example.ui.theme.LaborTextSecondary
import com.example.ui.viewmodel.LaborViewModel

@Composable
fun LoginScreen(
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var selectedGoogleEmail by remember { mutableStateOf("jyoti3322114455@gmail.com") }
    var selectedGoogleName by remember { mutableStateOf("Jyoti Manager") }
    var showSwitchAccountDialog by remember { mutableStateOf(false) }
    var customGoogleEmailInput by remember { mutableStateOf("") }
    var customGoogleNameInput by remember { mutableStateOf("") }
    var isSigningIn by remember { mutableStateOf(false) }

    val performSignIn: (String, String) -> Unit = { name, email ->
        isSigningIn = true
        viewModel.loginWithGoogle(
            name = name,
            email = email
        ) { success, msg ->
            isSigningIn = false
            Toast.makeText(
                context,
                msg,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D47A1),
                        LaborBlue,
                        Color(0xFFF4F6F9),
                        Color(0xFFFFFFFF)
                    ),
                    startY = 0f,
                    endY = 850f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // App Brand Logo & Title
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LB",
                    color = LaborBlue,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Laborbook",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("login_app_title")
            )

            Text(
                text = "Smart Attendance, Wages & Cash Book",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Main Google Authentication Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign in with Google",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = LaborTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Connect your verified Google Account to automatically backup & restore all your records to Google Drive.",
                        fontSize = 13.sp,
                        color = LaborTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Selected Google Account Badge (Clickable to sign in directly)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_selection_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF)),
                        border = BorderStroke(1.5.dp, LaborBlue.copy(alpha = 0.35f)),
                        onClick = {
                            performSignIn(selectedGoogleName, selectedGoogleEmail)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(LaborBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = selectedGoogleName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = selectedGoogleName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LaborTextPrimary
                                    )
                                    Text(
                                        text = selectedGoogleEmail,
                                        fontSize = 11.sp,
                                        color = LaborTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tap to login instantly",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LaborBlue
                                    )
                                }
                            }

                            TextButton(
                                onClick = { showSwitchAccountDialog = true },
                                modifier = Modifier.testTag("switch_google_account_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = LaborBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Switch",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborBlue
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Google Sign-In Action Button
                    Button(
                        onClick = {
                            performSignIn(selectedGoogleName, selectedGoogleEmail)
                        },
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_login_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Connecting Google Drive...", color = Color.White, fontSize = 14.sp)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Google G Badge
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        color = LaborBlue,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Continue with Google Account",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-restore status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LaborSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Automatic Google Drive sync & restore enabled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4B5563)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Cloud & Security Pillars Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AuthFeatureRow(
                        icon = Icons.Default.Restore,
                        iconColor = Color(0xFF0288D1),
                        title = "Automatic Google Drive Restore",
                        description = "When you log in with your Google account, your previous labor and cash data is automatically restored."
                    )
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    AuthFeatureRow(
                        icon = Icons.Default.CloudSync,
                        iconColor = LaborBlue,
                        title = "Continuous & Logout Backup",
                        description = "Every transaction and attendance mark updates your Drive backup. Auto-syncs before you log out."
                    )
                    HorizontalDivider(color = Color(0xFFF3F4F6))
                    AuthFeatureRow(
                        icon = Icons.Default.Shield,
                        iconColor = LaborSuccess,
                        title = "Separate Account Isolation",
                        description = "Each Google account keeps its own separate, encrypted dataset. One account can never access another's backup."
                    )
                }
            }
        }
    }

    // Switch Google Account Dialog
    if (showSwitchAccountDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = LaborBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Google Account", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Sign in with a verified Google account to isolate your records:",
                        fontSize = 12.sp,
                        color = LaborTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Verified Google Account
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedGoogleEmail == "jyoti3322114455@gmail.com") Color(0xFFEFF6FF) else Color(0xFFF9FAFB)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedGoogleEmail == "jyoti3322114455@gmail.com") LaborBlue else Color(0xFFE5E7EB)
                        ),
                        onClick = {
                            selectedGoogleEmail = "jyoti3322114455@gmail.com"
                            selectedGoogleName = "Jyoti Manager"
                            showSwitchAccountDialog = false
                            performSignIn("Jyoti Manager", "jyoti3322114455@gmail.com")
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4285F4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("J", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Jyoti Manager", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("jyoti3322114455@gmail.com", fontSize = 11.sp, color = LaborTextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Or enter another Google Account:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LaborTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customGoogleNameInput,
                        onValueChange = { customGoogleNameInput = it },
                        label = { Text("Account Name") },
                        placeholder = { Text("e.g. Ramesh Site Manager") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customGoogleEmailInput,
                        onValueChange = { customGoogleEmailInput = it },
                        label = { Text("Google Email (@gmail.com / Workspace)") },
                        placeholder = { Text("contractor@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = customGoogleEmailInput.trim()
                        if (email.isNotBlank()) {
                            if (!email.contains("@") || !email.contains(".")) {
                                Toast.makeText(context, "Please enter a valid Google email address", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val validEmail = email.lowercase()
                            val validName = if (customGoogleNameInput.isNotBlank()) customGoogleNameInput.trim() else email.substringBefore("@").replaceFirstChar { it.uppercase() }
                            selectedGoogleEmail = validEmail
                            selectedGoogleName = validName
                            showSwitchAccountDialog = false
                            performSignIn(validName, validEmail)
                        } else {
                            showSwitchAccountDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                ) {
                    Text("Sign In", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = LaborTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = LaborTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
