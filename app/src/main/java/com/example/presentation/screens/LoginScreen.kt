package com.example.presentation.screens

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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.theme.LaborBlue
import com.example.presentation.theme.LaborBlueDark
import com.example.presentation.theme.LaborTextPrimary
import com.example.presentation.theme.LaborTextSecondary
import com.example.presentation.viewmodel.LaborViewModel

import com.example.data.remote.FirebaseAuthHelper

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

    val isFirebaseAvailable = remember { FirebaseAuthHelper.isFirebaseInitialized(context) }

    val deepBlueBackground = Color(0xFF1E4665)
    val appFontFamily = FontFamily.SansSerif

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6)) // Light grey bottom background
    ) {
        // Deep Blue Header Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(deepBlueBackground)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Custom Image Logo
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_app_logo),
                    contentDescription = "Laborbook Logo",
                    modifier = Modifier.size(96.dp).clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Laborbook",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
                letterSpacing = 0.5.sp,
                modifier = Modifier.testTag("login_app_title")
            )

            Text(
                text = "Smart Attendance, Wages & Cash Book",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = appFontFamily
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("login_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { authMode = AuthMode.SIGN_IN },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Sign In",
                                fontSize = 16.sp,
                                fontWeight = if (authMode == AuthMode.SIGN_IN) FontWeight.Bold else FontWeight.Medium,
                                color = if (authMode == AuthMode.SIGN_IN) deepBlueBackground else LaborTextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp),
                                fontFamily = appFontFamily
                            )
                            if (authMode == AuthMode.SIGN_IN) {
                                HorizontalDivider(thickness = 3.dp, color = deepBlueBackground)
                            } else {
                                HorizontalDivider(thickness = 1.dp, color = Color.Transparent)
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { authMode = AuthMode.SIGN_UP },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Create Account",
                                fontSize = 16.sp,
                                fontWeight = if (authMode == AuthMode.SIGN_UP) FontWeight.Bold else FontWeight.Medium,
                                color = if (authMode == AuthMode.SIGN_UP) deepBlueBackground else LaborTextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp),
                                fontFamily = appFontFamily
                            )
                            if (authMode == AuthMode.SIGN_UP) {
                                HorizontalDivider(thickness = 3.dp, color = deepBlueBackground)
                            } else {
                                HorizontalDivider(thickness = 1.dp, color = Color.Transparent)
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Form Fields
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        
                        if (authMode == AuthMode.SIGN_UP) {
                            // Business Name
                            TextField(
                                value = businessNameInput,
                                onValueChange = { businessNameInput = it },
                                placeholder = { Text("Business / Contractor Name", fontFamily = appFontFamily) },
                                leadingIcon = {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = LaborTextSecondary, modifier = Modifier.size(20.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth().testTag("company_name_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = deepBlueBackground,
                                    unfocusedIndicatorColor = Color(0xFFE2E8F0),
                                    errorContainerColor = Color.Transparent
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = appFontFamily, fontSize = 16.sp, color = LaborTextPrimary)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Mobile
                            TextField(
                                value = mobileInput,
                                onValueChange = { mobileInput = it },
                                placeholder = { Text("Mobile Number (Optional)", fontFamily = appFontFamily) },
                                leadingIcon = {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = LaborTextSecondary, modifier = Modifier.size(20.dp))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth().testTag("mobile_number_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = deepBlueBackground,
                                    unfocusedIndicatorColor = Color(0xFFE2E8F0),
                                    errorContainerColor = Color.Transparent
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = appFontFamily, fontSize = 16.sp, color = LaborTextPrimary)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Email
                        TextField(
                            value = emailInput,
                            onValueChange = { 
                                emailInput = it
                                isEmailError = false
                            },
                            placeholder = { Text("Email Address", fontFamily = appFontFamily) },
                            isError = isEmailError,
                            leadingIcon = {
                                Icon(Icons.Outlined.Email, contentDescription = null, tint = if (isEmailError) androidx.compose.material3.MaterialTheme.colorScheme.error else LaborTextSecondary, modifier = Modifier.size(20.dp))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("email_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = deepBlueBackground,
                                unfocusedIndicatorColor = Color(0xFFE2E8F0),
                                errorContainerColor = Color.Transparent,
                                errorIndicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = appFontFamily, fontSize = 16.sp, color = LaborTextPrimary)
                        )
                        
                        if (isEmailError && emailErrorMessage.isNotBlank()) {
                            Text(
                                text = emailErrorMessage,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp),
                                textAlign = TextAlign.Start,
                                fontFamily = appFontFamily
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Password
                        TextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            placeholder = { Text("Password", fontFamily = appFontFamily) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = LaborTextSecondary, modifier = Modifier.size(20.dp))
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
                            modifier = Modifier.fillMaxWidth().testTag("password_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = deepBlueBackground,
                                unfocusedIndicatorColor = Color(0xFFE2E8F0),
                                errorContainerColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = appFontFamily, fontSize = 16.sp, color = LaborTextPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (authMode == AuthMode.SIGN_IN) {
                        Text(
                            text = "Forgot password?",
                            color = deepBlueBackground,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = appFontFamily,
                            modifier = Modifier.clickable { viewModel.resetPassword(context, emailInput.trim()) }
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val em = emailInput.trim().lowercase()
                            val pass = passwordInput.trim()

                            if (em.isBlank() || !em.contains("@") || !em.contains(".")) {
                                viewModel.showMessage("Please enter a valid email address")
                                return@Button
                            }
                            if (pass.length < 6) {
                                viewModel.showMessage("Password must be at least 6 characters")
                                return@Button
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
                            .height(52.dp)
                            .padding(horizontal = 16.dp)
                            .testTag("email_auth_submit_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = deepBlueBackground)
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Connecting...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = appFontFamily)
                        } else {
                            Text(
                                text = if (authMode == AuthMode.SIGN_IN) "Sign In with Email" else "Create Account with Email",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = appFontFamily
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        Text(
                            text = " OR WITH GOOGLE ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = LaborTextSecondary,
                            fontFamily = appFontFamily,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Secondary Google Button
                    OutlinedButton(
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
                                    viewModel.showMessage("Google Sign-In: $msg")
                                }
                            }
                        },
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 16.dp)
                            .testTag("google_login_button"),
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Using a simple "G" character to represent Google
                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_google_logo),
                                        contentDescription = "Google Logo",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                color = LaborTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = appFontFamily
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Minimal 3-Feature Trust Badges Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MinimalFeaturePill(
                    icon = Icons.Default.Sync,
                    text = "Auto Cloud Sync"
                )
                MinimalFeaturePill(
                    icon = Icons.Default.CloudDone,
                    text = "Instant Restore"
                )
                MinimalFeaturePill(
                    icon = Icons.Default.Shield,
                    text = "100% Private"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MinimalFeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif
        )
    }
}
