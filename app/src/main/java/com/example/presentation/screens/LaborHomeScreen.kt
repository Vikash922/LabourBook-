package com.example.presentation.screens

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.components.LaborWorkerCard
import com.example.presentation.components.LaborbookHomeTopBar
import com.example.presentation.theme.LaborBackground
import com.example.presentation.theme.LaborBlue
import com.example.presentation.theme.LaborSuccess
import com.example.presentation.theme.LaborError
import com.example.presentation.theme.LaborDivider
import com.example.presentation.theme.LaborTextSecondary
import com.example.presentation.viewmodel.LaborViewModel
import com.example.presentation.viewmodel.Screen
import com.example.core.util.AppStrings
import com.example.core.util.PdfReportGenerator

@Composable
fun LaborHomeScreen(
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workers by viewModel.filteredWorkers.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val lastBackupStatus by viewModel.lastBackupStatus.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val lastDeletedWorker by viewModel.lastDeletedWorker.collectAsStateWithLifecycle()
    val workerSearchQuery by viewModel.workerSearchQuery.collectAsStateWithLifecycle()
    val lang = userProfile.language

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LaborBackground,
        topBar = {
            LaborbookHomeTopBar(
                onShareClick = {
                    viewModel.shareBatchRoster()
                }
            )
        },
        floatingActionButton = {
            // 'ADD LABOR' Blue Filled Pill Button with person-add icon (spec 2.1)
            ExtendedFloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.AddLabor) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Labor",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = AppStrings.get("add_labor", lang).uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                containerColor = LaborBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(30.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("fab_add_labor")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            item {
                // Search Bar
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(12.dp))
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = workerSearchQuery,
                            onValueChange = { newValue: String ->
                                viewModel.onWorkerSearchQueryChanged(newValue)
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = @androidx.compose.runtime.Composable { innerTextField: @androidx.compose.runtime.Composable () -> Unit ->
                                androidx.compose.foundation.layout.Box {
                                    if (workerSearchQuery.isEmpty()) {
                                        Text(
                                            text = AppStrings.get("search_contact", lang),
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                // My Labour Title Label
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "My Labour",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                )



                // Accidentally Deleted Worker Undo Recovery Banner
                val showUndoBanner = lastDeletedWorker != null && workers.none { it.id == lastDeletedWorker?.id }
                if (showUndoBanner) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Deleted ${lastDeletedWorker?.name ?: "laborer"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Tap UNDO to restore immediately.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                            androidx.compose.material3.Button(
                                onClick = { viewModel.undoDeleteWorker() },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("UNDO", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Labor Worker Cards List
            if (workers.isEmpty()) {
                item {
                    EmptyLaborStateCard(
                        onAddLaborClick = { viewModel.navigateTo(Screen.AddLabor) },
                        onRestoreClick = {
                            viewModel.restoreFromSafetyBackup()
                        },
                        lastDeletedWorkerName = lastDeletedWorker?.name,
                        onUndoClick = { viewModel.undoDeleteWorker() },
                        lang = lang
                    )
                }
            } else {
                items(workers, key = { it.id }) { worker ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .shadow(elevation = 3.dp, shape = RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        com.example.presentation.components.LaborWorkerCard(
                            worker = worker,
                            onCardClick = { viewModel.navigateTo(Screen.LaborDetail(worker.id)) },
                            shape = RoundedCornerShape(10.dp),
                            showDivider = false
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun EmptyLaborStateCard(
    onAddLaborClick: () -> Unit,
    onRestoreClick: () -> Unit,
    lastDeletedWorkerName: String?,
    onUndoClick: () -> Unit,
    lang: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .testTag("empty_labor_state_card"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFEFF6FF), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = LaborBlue,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (lang.contains("Hindi") || lang.contains("हिंदी")) "कोई मजदूर नहीं जोड़ा गया" else "No Laborers Added Yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (lang.contains("Hindi") || lang.contains("हिंदी")) 
                    "मजदूरों को जोड़ने के लिए नीचे दिए गए बटन पर टैप करें। आप फोन संपर्कों से या सीधे नाम भरकर जोड़ सकते हैं।"
                    else "Add your laborers to start recording daily attendance, wage slips, advances, and payroll.",
                fontSize = 13.sp,
                color = LaborTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lastDeletedWorkerName != null) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onUndoClick,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706))
                    ) {
                        Text(
                            text = "Undo Delete ($lastDeletedWorkerName)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                androidx.compose.material3.Button(
                    onClick = onAddLaborClick,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = LaborBlue),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("empty_state_add_labor_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("add_labor", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            androidx.compose.material3.TextButton(
                onClick = onRestoreClick
            ) {
                Text(
                    text = "↺ Restore From Cloud / Safety Backup",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LaborBlue
                )
            }
        }
    }
}
