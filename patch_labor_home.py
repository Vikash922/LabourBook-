import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/LaborHomeScreen.kt', 'r') as f:
    content = f.read()

# Replace the single Card list of workers with individual outlined Cards
target_regex = r'\} else \{\s+item \{\s+Card\([\s\S]*?showDivider = !isLast\s+\)\s+\}\s+\}\s+\}\s+\}\s+\}'

new_code = """} else {
                items(workers, key = { it.id }) { worker ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        com.example.ui.components.LaborWorkerCard(
                            worker = worker,
                            onCardClick = { viewModel.navigateTo(Screen.LaborDetail(worker.id)) },
                            shape = RoundedCornerShape(16.dp),
                            showDivider = false
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }"""

content = re.sub(target_regex, new_code, content)

with open('/app/applet/app/src/main/java/com/example/ui/screens/LaborHomeScreen.kt', 'w') as f:
    f.write(content)
