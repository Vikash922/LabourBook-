import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/CashBookScreen.kt', 'r') as f:
    content = f.read()

# Replace the whole table block
table_regex = r'// Header Row[\s\S]*?item \{ Spacer\(modifier = Modifier\.height\(60\.dp\)\) \}'

new_table = """// Entire Table in one Card
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                        ) {
                            Column {
                                // Header Row
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
                                
                                // Items
                                displayTransactions.forEachIndexed { index, tx ->
                                    HorizontalDivider(color = Color(0xFFD1D5DB), thickness = 1.dp)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.openTransactionDetail(tx) }
                                            .testTag("tx_row_${tx.id}")
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
                                    }
                                }
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(60.dp)) }"""

content = re.sub(table_regex, new_table, content)

with open('/app/applet/app/src/main/java/com/example/ui/screens/CashBookScreen.kt', 'w') as f:
    f.write(content)
