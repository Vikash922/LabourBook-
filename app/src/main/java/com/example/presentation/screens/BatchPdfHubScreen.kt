package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.example.presentation.screens.SettingsGroupCard
import com.example.presentation.screens.SettingsRowItem
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.theme.LaborBackground
import com.example.presentation.theme.LaborBlue
import com.example.presentation.theme.LaborDivider
import com.example.presentation.theme.LaborSuccess
import com.example.presentation.theme.LaborTextPrimary
import com.example.presentation.theme.LaborTextSecondary
import com.example.presentation.viewmodel.LaborViewModel
import com.example.presentation.viewmodel.Screen

@Composable
fun BatchPdfHubScreen(
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val workers by viewModel.workers.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LaborBackground,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier.testTag("batch_hub_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Batch PDF Reports Hub",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Overview card
            item {
                SettingsGroupCard(title = "Consolidated Reports Hub") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Registered Staff", fontSize = 12.sp, color = LaborTextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${workers.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LaborBlue)
                        }
                        Column {
                            Text("Cash Book Entries", fontSize = 12.sp, color = LaborTextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${transactions.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LaborSuccess)
                        }
                        Column {
                            Text("Month", fontSize = 12.sp, color = LaborTextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(selectedMonth, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LaborTextPrimary)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Action 1 & 2: Batch PDF Reports
            item {
                SettingsGroupCard(title = "Batch PDF Reports") {
                    SettingsRowItem(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Monthly Staff Report",
                        subtitle = "Attendance, overtime & net wage",
                        iconTint = LaborBlue,
                        onClick = { viewModel.shareBatchRoster() }
                    )
                    
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    
                    SettingsRowItem(
                        icon = Icons.Default.ReceiptLong,
                        title = "Cash Book Ledger",
                        subtitle = "Inflow, outflow & net balance",
                        iconTint = Color(0xFF25D366),
                        onClick = { viewModel.shareCashBookReport() }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Action 3: Data Export & Backup (.CSV)
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                SettingsGroupCard(title = "Data Export & Backup (.CSV)") {
                    SettingsRowItem(
                        icon = Icons.Default.TableChart,
                        title = "Save to Device",
                        subtitle = "Download full .csv backup locally",
                        iconTint = Color(0xFF059669),
                        onClick = {
                            viewModel.saveCsvBackupToDevice(context) { success, msg ->
                                viewModel.showMessage(msg)
                            }
                        }
                    )
                    
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    
                    SettingsRowItem(
                        icon = Icons.Default.Share,
                        title = "Share .CSV File",
                        subtitle = "Export complete app data",
                        iconTint = Color(0xFF059669),
                        onClick = { viewModel.exportAndShareBackupCsv(context) }
                    )
                }
            }
        }
    }
}
