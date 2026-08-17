import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/CashBookScreen.kt', 'r') as f:
    content = f.read()

header_regex = r'// Header Row[\s\S]*?HorizontalDivider\(color = LaborDivider, thickness = 1\.dp\)\s+\}\s+\}\s+\}'
new_header = """// Header Row
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            color = Color.White,
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                        ) {
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
                                VerticalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())
                                Text(
                                    text = AppStrings.get("notes", lang),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black,
                                    modifier = Modifier.weight(0.45f).padding(horizontal = 12.dp, vertical = 12.dp)
                                )
                                VerticalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())
                                Text(
                                    text = AppStrings.get("amount", lang),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black,
                                    modifier = Modifier.weight(0.3f).padding(horizontal = 12.dp, vertical = 12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }"""
content = re.sub(header_regex, new_header, content)

item_regex = r'items\(displayTransactions, key = \{ it\.id \}\) \{ tx ->[\s\S]*?HorizontalDivider\(color = LaborDivider, thickness = 1\.dp\)\s+\}'
new_item = """itemsIndexed(displayTransactions, key = { _, tx -> tx.id }) { index, tx ->
                        val isLast = index == displayTransactions.lastIndex
                        Surface(
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { viewModel.openTransactionDetail(tx) }
                                .testTag("tx_row_${tx.id}"),
                            shape = if (isLast) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp) else androidx.compose.ui.graphics.RectangleShape
                        ) {
                            Column {
                                HorizontalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left border
                                    VerticalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())
                                    
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

                                    VerticalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())

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

                                    VerticalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())

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
                                    
                                    // Right border
                                    VerticalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp, modifier = Modifier.fillMaxHeight())
                                }
                                if (isLast) {
                                    HorizontalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp)
                                }
                            }
                        }
                    }"""

content = re.sub(item_regex, new_item, content)

# Need to ensure itemsIndexed is imported
if "import androidx.compose.foundation.lazy.itemsIndexed" not in content:
    content = content.replace("import androidx.compose.foundation.lazy.items", "import androidx.compose.foundation.lazy.items\nimport androidx.compose.foundation.lazy.itemsIndexed\nimport androidx.compose.foundation.layout.fillMaxHeight")

with open('/app/applet/app/src/main/java/com/example/ui/screens/CashBookScreen.kt', 'w') as f:
    f.write(content)
