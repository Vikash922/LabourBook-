package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.LaborCalendarHelper
import com.example.domain.model.PaymentMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceAmountBottomSheet(
    workerName: String,
    day: Int,
    selectedMonth: String,
    initialAdvance: Double,
    initialNote: String,
    initialPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirm: (Double, String, PaymentMethod) -> Unit
) {
    var amountStr by remember {
        mutableStateOf(if (initialAdvance > 0) (if (initialAdvance % 1.0 == 0.0) initialAdvance.toInt().toString() else initialAdvance.toString()) else "")
    }
    var noteStr by remember { mutableStateOf(initialNote) }
    var selectedPaymentMethod by remember { mutableStateOf(initialPaymentMethod) }

    val (year, monthNum) = remember(selectedMonth) {
        LaborCalendarHelper.parseYearMonth(selectedMonth)
    }
    val monthShortName = remember(monthNum) {
        LaborCalendarHelper.monthsShort.getOrElse(monthNum - 1) { "Aug" }
    }
    val formattedDate = String.format("%s %02d, %d", monthShortName, day, year)

    val isAmountValid = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val amountFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(200)
        try {
            amountFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .pointerInput(Unit) {}
                .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 28.dp)
        ) {
            // Header Row: [ Advance amount ] (left)  &  [ Aug 01, 2026 + Close Icon ] (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advance amount",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF111827),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

                Spacer(modifier = Modifier.height(34.dp))

                // Center Row: [ ₹ | 0 ] (Large Amount Input) & [ Online | Cash ] (Toggle Switch)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: ₹ and Amount Input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "₹",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Teal vertical cursor line
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .height(38.dp)
                                .background(Color(0xFF0D9488))
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Input field for amount
                        BasicTextField(
                            value = amountStr,
                            onValueChange = { input ->
                                amountStr = input.filter { it.isDigit() || it == '.' }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            ),
                            modifier = Modifier.focusRequester(amountFocusRequester),
                            decorationBox = { innerTextField ->
                                if (amountStr.isEmpty()) {
                                    Text(
                                        text = "0",
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right: [ Online | Cash ] Toggle Pill with shadow
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.2.dp, Color(0xFF1D61D2)),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Online tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (selectedPaymentMethod == PaymentMethod.ONLINE) Color(0xFF1D61D2) else Color.Transparent
                                    )
                                    .clickable { selectedPaymentMethod = PaymentMethod.ONLINE }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Online",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedPaymentMethod == PaymentMethod.ONLINE) Color.White else Color(0xFF1D61D2)
                                )
                            }

                            // Cash tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (selectedPaymentMethod == PaymentMethod.CASH) Color(0xFF1D61D2) else Color.Transparent
                                    )
                                    .clickable { selectedPaymentMethod = PaymentMethod.CASH }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cash",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedPaymentMethod == PaymentMethod.CASH) Color.White else Color(0xFF1D61D2)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Notes Section
                Text(
                    text = "Notes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notes Input Box with shadow & clean border
                Surface(
                    shape = RoundedCornerShape(25.dp),
                    color = Color(0xFFF9FAFB),
                    border = BorderStroke(1.2.dp, Color(0xFFE5E7EB)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = noteStr,
                            onValueChange = { noteStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF111827)
                            ),
                            decorationBox = { innerTextField ->
                                if (noteStr.isEmpty()) {
                                    Text(
                                        text = "Eg.(Food, petrol, rent)",
                                        fontSize = 14.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (initialAdvance > 0 && onDelete != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(0.4f)
                                .height(52.dp)
                                .clickable { onDelete() },
                            shape = RoundedCornerShape(26.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Delete",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val finalAmount = amountStr.toDoubleOrNull() ?: 0.0
                                if (finalAmount <= 0.0 && initialAdvance <= 0.0 && noteStr.isBlank()) {
                                    onDismiss()
                                } else {
                                    onConfirm(finalAmount, noteStr, selectedPaymentMethod)
                                }
                            },
                            modifier = Modifier
                                .weight(0.6f)
                                .height(52.dp)
                                .shadow(if (isAmountValid) 4.dp else 0.dp, RoundedCornerShape(26.dp)),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAmountValid) Color(0xFF1D61D2) else Color(0xFFB5B8BE),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Ok",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Ok Action Button (Pill shaped, full width with shadow)
                    Button(
                        onClick = {
                            val finalAmount = amountStr.toDoubleOrNull() ?: 0.0
                            if (finalAmount <= 0.0 && initialAdvance <= 0.0 && noteStr.isBlank()) {
                                onDismiss()
                            } else {
                                onConfirm(finalAmount, noteStr, selectedPaymentMethod)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(if (isAmountValid) 4.dp else 0.dp, RoundedCornerShape(26.dp)),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAmountValid) Color(0xFF1D61D2) else Color(0xFFB5B8BE),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Ok",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
