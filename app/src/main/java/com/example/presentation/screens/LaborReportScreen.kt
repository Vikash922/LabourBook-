package com.example.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AttendanceStatus
import com.example.presentation.theme.LaborBackground
import com.example.presentation.theme.LaborBlue
import com.example.presentation.theme.LaborDivider
import com.example.presentation.theme.LaborError
import com.example.presentation.theme.LaborSuccess
import com.example.presentation.theme.LaborTextPrimary
import com.example.presentation.theme.LaborTextSecondary
import com.example.presentation.viewmodel.LaborViewModel
import com.example.presentation.viewmodel.Screen
import com.example.core.util.PdfReportGenerator

@Composable
fun LaborReportScreen(
    workerId: String,
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workers by viewModel.workers.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val worker = workers.firstOrNull { it.id == workerId }

    if (worker == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Worker not found")
        }
        return
    }

    val present = worker.getTotalPresent(selectedMonth)
    val absent = worker.getTotalAbsent(selectedMonth)
    val overtime = worker.getTotalOvertimeHours(selectedMonth)
    val advance = worker.getTotalAdvance(selectedMonth)
    val totalGross = (present * worker.dailyWage) + (overtime * (worker.dailyWage / 8.0) * 1.5)
    val netPayable = totalGross - advance

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.navigateTo(Screen.LaborDetail(worker.id)) },
                            modifier = Modifier.testTag("report_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${worker.name}'s Wage Report",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    IconButton(
                        onClick = { viewModel.shareWorkerReport(worker) },
                        modifier = Modifier.testTag("report_share_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = LaborBlue
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Pinned Bottom Actions: 'Share PDF' and 'WhatsApp Share'
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 'Share PDF' Blue Pill Button
                    Button(
                        onClick = { viewModel.shareWorkerReport(worker) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_pdf_btn"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share PDF",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // 'WhatsApp Share' Green Pill Button
                    Button(
                        onClick = { viewModel.shareWorkerReport(worker) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_whatsapp_btn"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LaborSuccess)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Summary Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Period: $selectedMonth",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborBlue
                                )
                                Text(
                                    text = worker.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborTextPrimary
                                )
                            }
                        }
                        Text(
                            text = "Phone: ${worker.phoneNumber}",
                            fontSize = 13.sp,
                            color = LaborTextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = LaborDivider, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Daily Wage Rate:", color = LaborTextSecondary)
                            Text("₹${worker.dailyWage.toInt()} / day", fontWeight = FontWeight.Bold, color = LaborTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Present Days:", color = LaborTextSecondary)
                            Text(
                                text = if (present % 1.0 == 0.0) "${present.toInt()} Days" else "$present Days",
                                fontWeight = FontWeight.Bold,
                                color = LaborSuccess
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Absent Days:", color = LaborTextSecondary)
                            Text("${absent.toInt()} Days", fontWeight = FontWeight.Bold, color = LaborError)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Overtime:", color = LaborTextSecondary)
                            Text("${overtime.toInt()} Hours", fontWeight = FontWeight.Bold, color = LaborTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gross Earnings:", color = LaborTextSecondary)
                            Text("₹${String.format("%.1f", totalGross)}", fontWeight = FontWeight.Bold, color = LaborTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Advance Disbursed:", color = LaborTextSecondary)
                            Text("- ₹${advance.toInt()}", fontWeight = FontWeight.Bold, color = LaborError)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = LaborDivider, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NET PAYABLE:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborBlue
                            )
                            Text(
                                text = "₹${String.format("%.1f", netPayable)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborBlue
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Printable / Shareable Text Preview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Formatted Slip Preview ($selectedMonth)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborTextPrimary
                            )

                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = LaborBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = PdfReportGenerator.generateWorkerReportText(worker, selectedMonth),
                                fontSize = 12.sp,
                                color = Color(0xFF374151),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
