import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/CashBookScreen.kt', 'r') as f:
    content = f.read()

# Add imports
content = content.replace("import androidx.compose.material3.HorizontalDivider", 
                          "import androidx.compose.material3.HorizontalDivider\nimport androidx.compose.material3.VerticalDivider\nimport androidx.compose.foundation.layout.IntrinsicSize")

# Replace header block
header_regex = r'// Header Row\s+item \{\s+Surface\([\s\S]*?HorizontalDivider\(color = LaborDivider, thickness = 0\.5\.dp\)\s+\}\s+\}\s+\}'
new_header = """// Header Row
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White
                        ) {
                            Column {
                                HorizontalDivider(color = LaborDivider, thickness = 1.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = AppStrings.get("date", lang),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black,
                                        modifier = Modifier.weight(0.25f).padding(vertical = 12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    VerticalDivider(color = LaborDivider, thickness = 1.dp)
                                    Text(
                                        text = AppStrings.get("notes", lang),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black,
                                        modifier = Modifier.weight(0.45f).padding(horizontal = 12.dp, vertical = 12.dp)
                                    )
                                    VerticalDivider(color = LaborDivider, thickness = 1.dp)
                                    Text(
                                        text = AppStrings.get("amount", lang),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black,
                                        modifier = Modifier.weight(0.3f).padding(horizontal = 12.dp, vertical = 12.dp)
                                    )
                                }
                                HorizontalDivider(color = LaborDivider, thickness = 1.dp)
                            }
                        }
                    }"""

content = re.sub(header_regex, new_header, content)

# Replace item block
item_regex = r'items\(displayTransactions, key = \{ it\.id \}\) \{ tx ->[\s\S]*?HorizontalDivider\(color = LaborDivider, thickness = 0\.5\.dp\)\s+\}'
new_item = """items(displayTransactions, key = { it.id }) { tx ->
                        Surface(
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openTransactionDetail(tx) }
                                .testTag("tx_row_${tx.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Date Column
                                Column(
                                    modifier = Modifier.weight(0.25f).padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val parts = tx.dateDisplay.split(" ")
                                    val dayPart = parts.firstOrNull() ?: "15"
                                    val weekPart = parts.getOrNull(1) ?: "Sat"

                                    Text(
                                        text = dayPart,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = weekPart,
                                        fontSize = 12.sp,
                                        color = LaborTextSecondary
                                    )
                                }

                                VerticalDivider(color = LaborDivider, thickness = 1.dp)

                                // Notes & Payment Method Caption
                                Column(
                                    modifier = Modifier.weight(0.45f).padding(horizontal = 12.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = tx.notes.ifBlank { if (tx.type == TransactionType.CASH_IN) "Income" else "Expense" },
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = tx.paymentMethod.name,
                                        fontSize = 12.sp,
                                        color = LaborTextSecondary
                                    )
                                }

                                VerticalDivider(color = LaborDivider, thickness = 1.dp)

                                // ₹ Amount + Chevron
                                Row(
                                    modifier = Modifier.weight(0.3f).padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "₹${tx.amount.toInt()}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.type == TransactionType.CASH_IN) LaborSuccess else LaborError
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = LaborTextHint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = LaborDivider, thickness = 1.dp)
                    }"""

content = re.sub(item_regex, new_item, content)

with open('/app/applet/app/src/main/java/com/example/ui/screens/CashBookScreen.kt', 'w') as f:
    f.write(content)
