package com.example.presentation.components

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import com.example.domain.model.CashTransaction
import com.example.domain.model.PaymentMethod
import com.example.domain.model.TransactionType
import com.example.presentation.theme.LaborBlue
import com.example.presentation.theme.LaborDivider
import com.example.presentation.theme.LaborError
import com.example.presentation.theme.LaborTextPrimary
import com.example.presentation.theme.LaborTextSecondary
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
    val scrollState = rememberScrollState()

    // Dimmed Overlay
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onCloseClick() }
            .imePadding()
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
                        .verticalScroll(scrollState)
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val focusManager = LocalFocusManager.current

    var amountText by remember {
        mutableStateOf(if (transaction != null && transaction.amount > 0) transaction.amount.toInt().toString() else "")
    }
    var notesText by remember {
        mutableStateOf(transaction?.notes ?: "")
    }
    var selectedMethod by remember {
        mutableStateOf(transaction?.paymentMethod ?: PaymentMethod.CASH)
    }
    var currentType by remember {
        mutableStateOf(transaction?.type ?: defaultType)
    }
    
    val defaultCal = remember { Calendar.getInstance() }
    val fullFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    var selectedFullDate by remember {
        mutableStateOf(transaction?.fullDate ?: fullFormat.format(defaultCal.time))
    }
    var selectedDateDisplay by remember {
        mutableStateOf(transaction?.dateDisplay ?: displayFormat.format(defaultCal.time))
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
            val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            selectedFullDate = fullFormat.format(cal.time)
            selectedDateDisplay = displayFormat.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val amountFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(200)
        try {
            amountFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
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
            // Header Row: 'Cash In' (bold left) & 'Aug 21, 2026 Edit + Close Icon'
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentType == TransactionType.CASH_IN) "Cash In" else "Cash Out",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { datePickerDialog.show() }
                            .testTag("pick_transaction_date_btn")
                    ) {
                        Text(
                            text = selectedDateDisplay,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D61E7)
                        )
                    }

                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            onClose()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                            .testTag("close_edit_sheet")
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

            Spacer(modifier = Modifier.height(28.dp))

            // Amount Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ₹ + BasicTextField
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

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = amountText,
                        onValueChange = { input ->
                            amountText = input.filter { it.isDigit() || it == '.' }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(amountFocusRequester)
                            .testTag("tx_amount_input"),
                        decorationBox = { innerTextField ->
                            if (amountText.isEmpty()) {
                                Text(
                                    text = "0",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Online / Cash Segmented Toggle
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.2.dp, Color(0xFF1D61E7)),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selectedMethod == PaymentMethod.ONLINE) Color(0xFF1D61E7) else Color.Transparent)
                                .clickable { selectedMethod = PaymentMethod.ONLINE }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("method_online_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Online",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMethod == PaymentMethod.ONLINE) Color.White else Color(0xFF1D61E7)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selectedMethod == PaymentMethod.CASH) Color(0xFF1D61E7) else Color.Transparent)
                                .clickable { selectedMethod = PaymentMethod.CASH }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("method_cash_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cash",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMethod == PaymentMethod.CASH) Color.White else Color(0xFF1D61E7)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notes Section
            Text(
                text = "Notes",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151)
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                        value = notesText,
                        onValueChange = { notesText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_notes_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF111827)
                        ),
                        decorationBox = { innerTextField ->
                            if (notesText.isEmpty()) {
                                Text(
                                    text = "Eg. (Party name, Building name, Area name)",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (transaction != null && transaction.id.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .weight(0.4f)
                            .height(52.dp)
                            .clickable {
                                focusManager.clearFocus()
                                onDelete(transaction.id)
                            }
                            .testTag("delete_tx_btn"),
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
                }

                val isAmountEntered = amountText.trim().isNotEmpty() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0
                Button(
                    onClick = {
                        if (!isAmountEntered) return@Button
                        focusManager.clearFocus()
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
                    enabled = isAmountEntered,
                    modifier = Modifier
                        .weight(if (transaction != null && transaction.id.isNotBlank()) 0.6f else 1f)
                        .height(52.dp)
                        .shadow(if (isAmountEntered) 4.dp else 0.dp, RoundedCornerShape(26.dp))
                        .testTag("save_tx_btn"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAmountEntered) Color(0xFF1D61E7) else Color(0xFFB5B8BE),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Save",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
