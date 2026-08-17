package com.example.ui.screens

import android.app.Activity
import android.accounts.AccountManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LaborBlue
import com.example.ui.theme.LaborTextPrimary
import com.example.ui.theme.LaborTextSecondary
import com.example.ui.viewmodel.LaborViewModel

enum class AuthMode {
    SIGN_IN,
    SIGN_UP
}

@Composable
fun LoginScreen(
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var businessNameInput by remember { mutableStateOf("") }
    var mobileInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isEmailError by remember { mutableStateOf(false) }
    var emailErrorMessage by remember { mutableStateOf("") }

    var isSigningIn by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Gradient Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            LaborBlue,
                            Color(0xFF1E40AF),
                            Color(0xFFF8FAFC)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Brand Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "LB",
                        color = LaborBlue,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Laborbook",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("login_app_title")
            )

            Text(
                text = "Smart Attendance, Wages & Cash Book",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google Sign-In Hero Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            isSigningIn = true
                            viewModel.signInWithGoogleCredentialManager(
                                context = context,
                                businessName = businessNameInput.ifBlank { "My Business" },
                                mobile = mobileInput
                            ) { success, msg ->
                                isSigningIn = false
                                if (!success) {
                                    Toast.makeText(context, "Google Sign-In: $msg", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
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
                            Text("Syncing with Firebase...", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "G",
                                            color = LaborBlue,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Continue with Google",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        Text(
                            text = " OR WITH EMAIL ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaborTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Segmented Mode Selector (Sign In vs Create Account)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (authMode == AuthMode.SIGN_IN) Color.White else Color.Transparent)
                                    .clickable { authMode = AuthMode.SIGN_IN }
                                    .testTag("tab_sign_in"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign In",
                                    fontSize = 13.sp,
                                    fontWeight = if (authMode == AuthMode.SIGN_IN) FontWeight.Bold else FontWeight.Medium,
                                    color = if (authMode == AuthMode.SIGN_IN) LaborBlue else LaborTextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (authMode == AuthMode.SIGN_UP) Color.White else Color.Transparent)
                                    .clickable { authMode = AuthMode.SIGN_UP }
                                    .testTag("tab_sign_up"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Create Account",
                                    fontSize = 13.sp,
                                    fontWeight = if (authMode == AuthMode.SIGN_UP) FontWeight.Bold else FontWeight.Medium,
                                    color = if (authMode == AuthMode.SIGN_UP) LaborBlue else LaborTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Fields based on AuthMode
                    if (authMode == AuthMode.SIGN_UP) {
                        // Business / Contractor Name Input
                        OutlinedTextField(
                            value = businessNameInput,
                            onValueChange = { businessNameInput = it },
                            label = { Text("Business / Contractor Name") },
                            placeholder = { Text("e.g. Acme Construction") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = LaborBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("company_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LaborBlue,
                                unfocusedBorderColor = Color(0xFFD1D5DB)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mobile Number Input
                        OutlinedTextField(
                            value = mobileInput,
                            onValueChange = { mobileInput = it },
                            label = { Text("Mobile Number (Optional)") },
                            placeholder = { Text("e.g. 9876543210") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = LaborBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mobile_number_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LaborBlue,
                                unfocusedBorderColor = Color(0xFFD1D5DB)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Email Input
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { 
                            emailInput = it
                            isEmailError = false
                        },
                        label = { Text("Email Address") },
                        placeholder = { Text("example@gmail.com") },
                        isError = isEmailError,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = if (isEmailError) androidx.compose.material3.MaterialTheme.colorScheme.error else LaborBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LaborBlue,
                            unfocusedBorderColor = Color(0xFFD1D5DB)
                        )
                    )
                    
                    if (isEmailError && emailErrorMessage.isNotBlank()) {
                        Text(
                            text = emailErrorMessage,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 4.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Input
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        placeholder = { Text("At least 6 characters") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = LaborBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = LaborTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LaborBlue,
                            unfocusedBorderColor = Color(0xFFD1D5DB)
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Button (Sign In / Sign Up)
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            val em = emailInput.trim().lowercase()
                            val pass = passwordInput.trim()

                            if (em.isBlank() || !em.contains("@") || !em.contains(".")) {
                                Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            if (pass.length < 6) {
                                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }

                            isSigningIn = true
                            if (authMode == AuthMode.SIGN_IN) {
                                viewModel.signInWithEmail(
                                    context = context,
                                    email = em,
                                    pass = pass,
                                    businessName = businessNameInput,
                                    mobile = mobileInput
                                ) { success, msg ->
                                    isSigningIn = false
                                    if (!success) {
                                        isEmailError = true
                                        emailErrorMessage = "Invalid user email or password. Please create an account."
                                    }
                                }
                            } else {
                                val bizName = businessNameInput.trim().ifBlank { "My Business" }
                                viewModel.signUpWithEmail(
                                    context = context,
                                    email = em,
                                    pass = pass,
                                    businessName = bizName,
                                    mobile = mobileInput.trim()
                                ) { success, msg ->
                                    isSigningIn = false
                                    if (!success) {
                                        isEmailError = true
                                        emailErrorMessage = msg
                                    }
                                }
                            }
                        },
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("email_auth_submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, LaborBlue),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = LaborBlue
                        )
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = LaborBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Connecting...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (authMode == AuthMode.SIGN_IN) Icons.Default.Email else Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = LaborBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (authMode == AuthMode.SIGN_IN) "Sign In with Email" else "Create Account with Email",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Minimal 3-Feature Trust Badges Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MinimalFeaturePill(
                    icon = Icons.Default.Sync,
                    text = "Auto Cloud Sync",
                    modifier = Modifier.weight(1f)
                )
                MinimalFeaturePill(
                    icon = Icons.Default.CloudDone,
                    text = "Instant Restore",
                    modifier = Modifier.weight(1f)
                )
                MinimalFeaturePill(
                    icon = Icons.Default.Shield,
                    text = "100% Private",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MinimalFeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LaborBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = LaborTextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}
