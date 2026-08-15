package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.theme.LaborBackground
import com.example.ui.theme.LaborBlue
import com.example.ui.theme.LaborDivider
import com.example.ui.theme.LaborError
import com.example.ui.theme.LaborSuccess
import com.example.ui.theme.LaborTextPrimary
import com.example.ui.theme.LaborTextSecondary
import com.example.ui.viewmodel.LaborViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun CashBookReportScreen(
    viewModel: LaborViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()

    val cashInTotal = transactions.filter { it.type == TransactionType.CASH_IN }.sumOf { it.amount }
    val cashOutTotal = transactions.filter { it.type == TransactionType.CASH_OUT }.sumOf { it.amount }
    val balance = cashInTotal - cashOutTotal

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LaborBackground,
        topBar = {
            // App Bar: White background, back arrow, 'Report' title, WhatsApp share icon (spec 2.7 Image 9)
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
                            onClick = { viewModel.navigateTo(Screen.CashBook) },
                            modifier = Modifier.testTag("cb_report_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Report",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    IconButton(
                        onClick = { viewModel.shareCashBookReport() },
                        modifier = Modifier.testTag("cb_report_share_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(0xFF25D366) // WhatsApp green
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Pinned Bottom Buttons: 'Download' (blue pill) & 'Share' (green WhatsApp pill) (spec 2.7)
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
                    // 'Download' Button
                    Button(
                        onClick = { viewModel.shareCashBookReport() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("download_report_btn"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Download",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // 'Share' WhatsApp Button
                    Button(
                        onClick = { viewModel.shareCashBookReport() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_cb_report_btn"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share",
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
            contentPadding = PaddingValues(16.dp)
        ) {
            // Duration Bar: 'Duration' on left, date range pill on right (spec 2.7)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Duration",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LaborTextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LaborDivider)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = LaborBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sat, 01 Aug 26 - Mon, 31 Aug 26",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LaborTextPrimary
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Summary Card (Cash In, Cash Out, Balance)
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
                            Text("Cash In", fontSize = 15.sp, color = LaborTextSecondary)
                            Text("₹ ${cashInTotal.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LaborSuccess)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cash Out", fontSize = 15.sp, color = LaborTextSecondary)
                            Text("₹ ${cashOutTotal.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LaborError)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = LaborDivider, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Balance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LaborTextPrimary)
                            Text("₹ ${balance.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (balance >= 0) LaborBlue else LaborError)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Breakdown List Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FAFB))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaborTextSecondary,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = "Notes & Method",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaborTextSecondary,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = "Amount",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaborTextSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                    HorizontalDivider(color = LaborDivider, thickness = 0.8.dp)
                }
            }

            items(transactions, key = { it.id }) { tx ->
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = tx.dateDisplay,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborTextPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(2f)) {
                            Text(
                                text = tx.notes.ifBlank { if (tx.type == TransactionType.CASH_IN) "Income" else "Expense" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborTextPrimary
                            )
                            Text(
                                text = tx.paymentMethod.name,
                                fontSize = 11.sp,
                                color = LaborTextSecondary
                            )
                        }
                        Text(
                            text = (if (tx.type == TransactionType.CASH_IN) "+ ₹" else "- ₹") + "${tx.amount.toInt()}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tx.type == TransactionType.CASH_IN) LaborSuccess else LaborError,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 0.8.dp)
            }
        }
    }
}
