package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CashTransaction
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType
import com.example.ui.theme.LaborBlue
import com.example.ui.theme.LaborDivider
import com.example.ui.theme.LaborError
import com.example.ui.theme.LaborTextPrimary
import com.example.ui.theme.LaborTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TransactionViewBottomSheet(
    transaction: CashTransaction,
    onEditClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dimmed Overlay
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onCloseClick() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clickable(enabled = false) {}
                .fillMaxWidth()
        ) {
            // Floating Close (X) Circular Button top-right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onCloseClick() }
                        .testTag("close_view_sheet")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Sheet Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Header Row: Date Title & Edit Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = LaborBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = transaction.fullDate.ifBlank { "Aug 15, 2026" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborTextPrimary
                            )
                        }

                        // 'Edit' outline pill button
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, LaborDivider),
                            modifier = Modifier
                                .clickable { onEditClick() }
                                .testTag("edit_transaction_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = LaborBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LaborBlue
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Detail Rows (Label left, Value right)
                    DetailRow(
                        label = "Transaction Type:",
                        value = if (transaction.type == TransactionType.CASH_IN) "Cash In" else "Cash Out"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DetailRow(
                        label = "Amount:",
                        value = "₹ ${transaction.amount.toInt()}"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DetailRow(
                        label = "Payment Method:",
                        value = if (transaction.paymentMethod == PaymentMethod.CASH) "Cash" else "Online"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DetailRow(
                        label = "Notes:",
                        value = transaction.notes.ifBlank { "None" }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 'Ok' Full-Width Stadium Blue Button
                    Button(
                        onClick = { onCloseClick() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("ok_transaction_btn"),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
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
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = LaborTextSecondary
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = LaborTextPrimary
        )
    }
}

@Composable
fun TransactionEditBottomSheet(
    transaction: CashTransaction?,
    defaultType: TransactionType = TransactionType.CASH_IN,
    onSave: (id: String, amount: Double, method: PaymentMethod, notes: String, type: TransactionType, dateDisplay: String, fullDate: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var amountText by remember {
        mutableStateOf(if (transaction != null && transaction.amount > 0) transaction.amount.toInt().toString() else "500")
    }
    var notesText by remember {
        mutableStateOf(transaction?.notes ?: if (defaultType == TransactionType.CASH_IN) "Income" else "Expense")
    }
    var selectedMethod by remember {
        mutableStateOf(transaction?.paymentMethod ?: PaymentMethod.CASH)
    }
    var currentType by remember {
        mutableStateOf(transaction?.type ?: defaultType)
    }
    var selectedFullDate by remember {
        mutableStateOf(transaction?.fullDate ?: "2026-08-15")
    }
    var selectedDateDisplay by remember {
        mutableStateOf(transaction?.dateDisplay ?: "15 Sat")
    }

    // Android Calendar Date Picker
    val calendar = Calendar.getInstance()
    try {
        val parts = selectedFullDate.split("-")
        if (parts.size == 3) {
            calendar.set(Calendar.YEAR, parts[0].toInt())
            calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
            calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
        }
    } catch (e: Exception) {
        // use default
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            val fullFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("dd EEE", Locale.getDefault())
            selectedFullDate = fullFormat.format(cal.time)
            selectedDateDisplay = displayFormat.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClose() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clickable(enabled = false) {}
                .fillMaxWidth()
        ) {
            // Floating Close (X)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onClose() }
                        .testTag("close_edit_sheet")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Sheet
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Header Row: 'Cash In' (bold left) | Interactive Date Pill with Calendar Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (currentType == TransactionType.CASH_IN) "Cash In" else "Cash Out",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = LaborTextPrimary
                        )

                        // Date Pill (Click to open Calendar Date Picker)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF3F4F6),
                            border = BorderStroke(1.dp, LaborDivider),
                            modifier = Modifier
                                .clickable { datePickerDialog.show() }
                                .testTag("pick_transaction_date_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Change Date",
                                    tint = LaborBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedFullDate,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborBlue
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Large Amount Display & Online / Cash Segmented Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Amount input with rupee symbol
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "₹",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = LaborTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborTextPrimary
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .width(130.dp)
                                    .testTag("tx_amount_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LaborBlue,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                        }

                        // Online / Cash Segmented Toggle
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF3F4F6),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (selectedMethod == PaymentMethod.ONLINE) Color.White else Color.Transparent)
                                        .clickable { selectedMethod = PaymentMethod.ONLINE }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .testTag("method_online_btn")
                                ) {
                                    Text(
                                        text = "Online",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedMethod == PaymentMethod.ONLINE) LaborBlue else LaborTextSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (selectedMethod == PaymentMethod.CASH) Color.White else Color.Transparent)
                                        .clickable { selectedMethod = PaymentMethod.CASH }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .testTag("method_cash_btn")
                                ) {
                                    Text(
                                        text = "Cash",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedMethod == PaymentMethod.CASH) LaborBlue else LaborTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notes Input Field
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes") },
                        placeholder = { Text("Enter note (e.g. Income, Cement purchase)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_notes_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LaborBlue,
                            unfocusedBorderColor = LaborDivider
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bottom Row: 'Delete Entry' and 'Save' buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (transaction != null && transaction.id.isNotBlank()) {
                            Button(
                                onClick = { onDelete(transaction.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("delete_tx_btn"),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = LaborError
                                ),
                                border = BorderStroke(1.2.dp, LaborError)
                            ) {
                                Text(
                                    text = "Delete Entry",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LaborError
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                onSave(
                                    transaction?.id ?: "",
                                    amount,
                                    selectedMethod,
                                    notesText.trim(),
                                    currentType,
                                    selectedDateDisplay,
                                    selectedFullDate
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_tx_btn"),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LaborBlue)
                        ) {
                            Text(
                                text = "Save",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
