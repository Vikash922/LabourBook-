package com.example.presentation.components

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.LaborWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkerProfileBottomSheet(
    worker: LaborWorker,
    onDismiss: () -> Unit,
    onSave: (newName: String, newSalary: Double, salaryType: String) -> Unit
) {
    var nameInput by remember { mutableStateOf(worker.name) }
    var selectedSalaryType by remember { mutableStateOf("Daily") } // "Daily" or "Monthly"
    
    val initialSalaryStr = remember(worker.dailyWage) {
        if (worker.dailyWage > 0) {
            if (worker.dailyWage % 1.0 == 0.0) worker.dailyWage.toInt().toString() else worker.dailyWage.toString()
        } else ""
    }
    var salaryInput by remember { mutableStateOf(initialSalaryStr) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isValid = nameInput.trim().isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 12.dp)
            ) {
                // Header: Edit Profile
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(2.dp, CircleShape)
                            .background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF1D61D2),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Edit Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Staff name label
                Text(
                    text = "Staff name",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Staff name text input capsule with soft shadow & crisp border
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFFF9FAFB),
                    border = BorderStroke(1.2.dp, Color(0xFFE5E7EB)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF111827)
                            ),
                            decorationBox = { innerTextField ->
                                if (nameInput.isEmpty()) {
                                    Text(
                                        text = "Enter staff name",
                                        fontSize = 16.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Salary type label
                Text(
                    text = "Salary type",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Radio options row: Daily & Monthly styled as sleek rounded cards with shadows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Daily Option Card
                    val isDaily = selectedSalaryType == "Daily"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedSalaryType = "Daily" },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDaily) Color(0xFFF0FDF4) else Color(0xFFF9FAFB),
                        border = BorderStroke(
                            1.2.dp,
                            if (isDaily) Color(0xFF0D9488) else Color(0xFFE5E7EB)
                        ),
                        shadowElevation = if (isDaily) 2.dp else 1.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            RadioButton(
                                selected = isDaily,
                                onClick = { selectedSalaryType = "Daily" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF0D9488),
                                    unselectedColor = Color(0xFF9CA3AF)
                                ),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily",
                                fontSize = 15.sp,
                                fontWeight = if (isDaily) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDaily) Color(0xFF0F766E) else Color(0xFF374151)
                            )
                        }
                    }

                    // Monthly Option Card
                    val isMonthly = selectedSalaryType == "Monthly"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedSalaryType = "Monthly" },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isMonthly) Color(0xFFF0FDF4) else Color(0xFFF9FAFB),
                        border = BorderStroke(
                            1.2.dp,
                            if (isMonthly) Color(0xFF0D9488) else Color(0xFFE5E7EB)
                        ),
                        shadowElevation = if (isMonthly) 2.dp else 1.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            RadioButton(
                                selected = isMonthly,
                                onClick = { selectedSalaryType = "Monthly" },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF0D9488),
                                    unselectedColor = Color(0xFF9CA3AF)
                                ),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monthly",
                                fontSize = 15.sp,
                                fontWeight = if (isMonthly) FontWeight.Bold else FontWeight.Medium,
                                color = if (isMonthly) Color(0xFF0F766E) else Color(0xFF374151)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Enter salary amount label
                Text(
                    text = "Enter salary amount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Salary amount input capsule with subtle shadow
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFFF9FAFB),
                    border = BorderStroke(1.2.dp, Color(0xFFE5E7EB)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = salaryInput,
                            onValueChange = { input ->
                                salaryInput = input.filter { it.isDigit() || it == '.' }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF111827)
                            ),
                            decorationBox = { innerTextField ->
                                if (salaryInput.isEmpty()) {
                                    Text(
                                        text = if (selectedSalaryType == "Daily") "₹ Enter daily salary amount" else "₹ Enter monthly salary amount",
                                        fontSize = 15.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₹ ",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111827)
                                        )
                                        innerTextField()
                                    }
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Save button with shadow
                Button(
                    onClick = {
                        val parsedSalary = salaryInput.toDoubleOrNull() ?: worker.dailyWage
                        onSave(nameInput.trim(), parsedSalary, selectedSalaryType)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(if (isValid) 4.dp else 0.dp, RoundedCornerShape(26.dp)),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isValid) Color(0xFF1D61D2) else Color(0xFFB5B8BE),
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

            // Floating Close Button (top-right)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 16.dp)
                    .size(36.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape)
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
}
