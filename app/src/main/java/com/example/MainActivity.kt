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
import com.example.ui.components.LaborbookBottomNav
import com.example.ui.screens.AddLaborScreen
import com.example.ui.screens.BatchPdfHubScreen
import com.example.ui.screens.CashBookReportScreen
import com.example.ui.screens.CashBookScreen
import com.example.ui.screens.LaborDetailScreen
import com.example.ui.screens.LaborHomeScreen
import com.example.ui.screens.LaborReportScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.LaborbookTheme
import com.example.ui.viewmodel.LaborViewModel
import com.example.ui.viewmodel.Screen

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
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedTab by viewModel.selectedTabIndex.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
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
                        .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = com.example.ui.theme.LaborBlue, spotColor = com.example.ui.theme.LaborBlue)
                        .background(Color(0xFF1E293B), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Notification",
                        tint = com.example.ui.theme.LaborWarning,
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
