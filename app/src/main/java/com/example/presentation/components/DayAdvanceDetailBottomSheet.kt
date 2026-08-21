package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.LaborCalendarHelper
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.PaymentMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayAdvanceDetailBottomSheet(
    day: Int,
    selectedMonth: String,
    status: AttendanceStatus,
    advanceAmount: Double,
    paymentMethod: PaymentMethod,
    note: String,
    onEditClicked: () -> Unit,
    onDeleteClicked: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val (year, month) = LaborCalendarHelper.parseYearMonth(selectedMonth)
    val monthName = try {
        selectedMonth.split(" ").firstOrNull() ?: "Aug"
    } catch (e: Exception) {
        "Aug"
    }
    val yearName = try {
        selectedMonth.split(" ").lastOrNull() ?: "2026"
    } catch (e: Exception) {
        "2026"
    }
    val formattedDate = "${monthName.take(3)} ${String.format("%02d", day)}, $yearName"

    val formattedAdvance = if (advanceAmount % 1.0 == 0.0) {
        advanceAmount.toInt().toString()
    } else {
        advanceAmount.toString()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Top Header: Date on left, Edit Button on right + Close Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Title (e.g., "Aug 01, 2026")
                Text(
                    text = formattedDate,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onDeleteClicked != null) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(0.8.dp, Color(0xFFFECACA)),
                            shadowElevation = 0.5.dp
                        ) {
                            IconButton(
                                onClick = onDeleteClicked,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Advance",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    // Edit pill button
                    Surface(
                        modifier = Modifier.clickable { onEditClicked() },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                        color = Color.White,
                        shadowElevation = 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Advance",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    // Floating circular close button
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Row 1: Attendance
            DetailRowItem(
                label = "Attendance",
                valueContent = {
                    val (badgeBg, badgeText) = when (status) {
                        AttendanceStatus.PRESENT -> Pair(Color(0xFF10B981), "P")
                        AttendanceStatus.ABSENT -> Pair(Color(0xFFEF4444), "A")
                        AttendanceStatus.HALF_DAY -> Pair(Color(0xFFF59E0B), "½")
                        AttendanceStatus.PRESENT_HALF -> Pair(Color(0xFF06B6D4), "P + ½")
                        AttendanceStatus.DOUBLE -> Pair(Color(0xFF2563EB), "P + P")
                        AttendanceStatus.PAID_LEAVE -> Pair(Color(0xFF7C3AED), "PA")
                        AttendanceStatus.OVERTIME -> Pair(Color(0xFF7E3B7D), "OT")
                        AttendanceStatus.UNMARKED -> Pair(Color(0xFF94A3B8), "-")
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeBg,
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            )

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(vertical = 14.dp))

            // Row 2: Advance amount
            DetailRowItem(
                label = "Advance amount",
                valueContent = {
                    Text(
                        text = "₹ $formattedAdvance",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            )

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(vertical = 14.dp))

            // Row 3: Payment Method
            DetailRowItem(
                label = "Payment Method",
                valueContent = {
                    Text(
                        text = if (paymentMethod == PaymentMethod.ONLINE) "Online" else "Cash",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            )

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(vertical = 14.dp))

            // Row 4: Notes
            DetailRowItem(
                label = "Notes",
                valueContent = {
                    Text(
                        text = if (note.isNotBlank()) note else "-",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (note.isNotBlank()) Color(0xFF0F172A) else Color(0xFF94A3B8)
                    )
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Action: Ok Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D61D2)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Ok",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRowItem(
    label: String,
    valueContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF334155)
        )
        valueContent()
    }
}
