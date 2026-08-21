package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import com.example.presentation.components.LaborbookBottomNav
import com.example.presentation.screens.AddLaborScreen
import com.example.presentation.screens.BatchPdfHubScreen
import com.example.presentation.screens.CashBookReportScreen
import com.example.presentation.screens.CashBookScreen
import com.example.presentation.screens.LaborDetailScreen
import com.example.presentation.screens.LaborHomeScreen
import com.example.presentation.screens.LaborReportScreen
import com.example.presentation.screens.LoginScreen
import com.example.presentation.screens.SettingsScreen
import com.example.presentation.screens.SplashScreen
import com.example.presentation.theme.LaborbookTheme
import com.example.presentation.viewmodel.LaborViewModel
import com.example.presentation.viewmodel.Screen

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.getInstance

class MainActivity : ComponentActivity() {
    private val viewModel: LaborViewModel by viewModels()
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Firebase Analytics
        firebaseAnalytics = getInstance(this)
        
        // Check if Firebase user is authenticated on startup and navigate to dashboard
        viewModel.checkFirebaseAutoLogin(isStartup = true)
        setContent {
            LaborbookTheme {
                LaborbookApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkFirebaseAutoLogin(isStartup = false)
    }
}

@Composable
fun LaborbookApp(viewModel: LaborViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTabIndex.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMessage) {
        syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearSyncMessage()
        }
    }

    val isRootTabScreen = currentScreen is Screen.LaborHome ||
            currentScreen is Screen.CashBook ||
            currentScreen is Screen.Settings

    BackHandler(enabled = !isRootTabScreen) {
        when (currentScreen) {
            is Screen.AddLabor -> viewModel.navigateTo(Screen.LaborHome)
            is Screen.LaborDetail -> viewModel.navigateTo(Screen.LaborHome)
            is Screen.LaborReport -> {
                val workerId = (currentScreen as Screen.LaborReport).workerId
                viewModel.navigateTo(Screen.LaborDetail(workerId))
            }
            is Screen.CashBookReport -> viewModel.navigateTo(Screen.CashBook)
            is Screen.BatchPdfHub -> viewModel.navigateTo(Screen.Settings)
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { 
            SnackbarHost(snackbarHostState) { data ->
                Row(
                    modifier = Modifier
                        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = com.example.presentation.theme.LaborBlue, spotColor = com.example.presentation.theme.LaborBlue)
                        .background(Color(0xFF1E293B), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Notification",
                        tint = com.example.presentation.theme.LaborWarning,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = data.visuals.message,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        bottomBar = {
            if (isRootTabScreen) {
                LaborbookBottomNav(
                    selectedTabIndex = selectedTab,
                    onTabSelected = { index ->
                        viewModel.selectTab(index)
                    },
                    language = userProfile.language
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                label = "ScreenTransition",
                transitionSpec = {
                    val isRootToRoot = (initialState is Screen.LaborHome || initialState is Screen.CashBook || initialState is Screen.Settings) &&
                            (targetState is Screen.LaborHome || targetState is Screen.CashBook || targetState is Screen.Settings)
                    
                    val isLogin = initialState is Screen.Login || targetState is Screen.Login
                    
                    if (isRootToRoot || isLogin) {
                        fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                    } else {
                        val isNavigatingBack = (targetState is Screen.LaborHome && (initialState is Screen.AddLabor || initialState is Screen.LaborDetail)) ||
                                (targetState is Screen.CashBook && initialState is Screen.CashBookReport) ||
                                (targetState is Screen.Settings && initialState is Screen.BatchPdfHub) ||
                                (targetState is Screen.LaborDetail && initialState is Screen.LaborReport)

                        if (isNavigatingBack) {
                            (slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(180)
                            ) + fadeIn(animationSpec = tween(180))) togetherWith (slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(180)
                            ) + fadeOut(animationSpec = tween(180)))
                        } else {
                            (slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(180)
                            ) + fadeIn(animationSpec = tween(180))) togetherWith (slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(180)
                            ) + fadeOut(animationSpec = tween(180)))
                        }
                    }
                }
            ) { screen ->
                when (screen) {
                    is Screen.Splash -> SplashScreen(onFinished = { viewModel.onSplashFinished() })
                    is Screen.Login -> LoginScreen(viewModel = viewModel)
                    is Screen.LaborHome -> LaborHomeScreen(viewModel = viewModel)
                    is Screen.AddLabor -> AddLaborScreen(viewModel = viewModel)
                    is Screen.LaborDetail -> LaborDetailScreen(workerId = screen.workerId, viewModel = viewModel)
                    is Screen.LaborReport -> LaborReportScreen(workerId = screen.workerId, viewModel = viewModel)
                    is Screen.CashBook -> CashBookScreen(viewModel = viewModel)
                    is Screen.CashBookReport -> CashBookReportScreen(viewModel = viewModel)
                    is Screen.Settings -> SettingsScreen(viewModel = viewModel)
                    is Screen.BatchPdfHub -> BatchPdfHubScreen(viewModel = viewModel)
                }
            }
        }
    }
}
