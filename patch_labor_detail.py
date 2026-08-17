import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/LaborDetailScreen.kt', 'r') as f:
    content = f.read()

# Add imports
imports_to_add = """
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
"""
content = re.sub(r'(import androidx.compose.material3.CardDefaults)', r'\1' + imports_to_add, content)


# Replace the Delete Worker Confirmation Dialog with the new ModalBottomSheet
dialog_regex = r'// Delete Worker Confirmation Dialog\s+if \(showDeleteConfirmDialog\) \{[\s\S]*?showEditWorkerDialog\) \{'

bottom_sheet_ui = """@OptIn(ExperimentalMaterial3Api::class)
    // Delete Worker Confirmation Dialog
    if (showDeleteConfirmDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showDeleteConfirmDialog = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Floating Close Button (top right)
                IconButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp, top = 0.dp)
                        .size(40.dp)
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Black
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
                ) {
                    // Initial Letter
                    Text(
                        text = worker.initial,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Worker Name
                    Text(
                        text = worker.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Swipe to Delete Component
                    SwipeToConfirm(
                        onConfirm = {
                            viewModel.deleteWorker(worker.id)
                            showDeleteConfirmDialog = false
                            viewModel.navigateTo(Screen.LaborHome)
                        }
                    )
                }
            }
        }
    }

    // Edit Worker Dialog
    if (showEditWorkerDialog) {"""

content = re.sub(dialog_regex, bottom_sheet_ui, content)

# Append the SwipeToConfirm component at the end of the file
swipe_to_confirm_code = """

@Composable
fun SwipeToConfirm(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var isConfirmed by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFB91C1C)) // Dark red background
    ) {
        // We subtract the thumb size (56dp) to get the max swipe distance
        val maxSwipePx = with(density) { (maxWidth - 56.dp).toPx() }
        
        // Background text
        Text(
            text = "Swipe to Delete Labor",
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Thumb (circular button)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                .padding(4.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF7F1D1D)) // Darker red thumb
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeOffset > maxSwipePx * 0.8f) {
                                swipeOffset = maxSwipePx
                                if (!isConfirmed) {
                                    isConfirmed = true
                                    onConfirm()
                                }
                            } else {
                                swipeOffset = 0f
                            }
                        }
                    ) { change, dragAmount ->
                        if (!isConfirmed) {
                            swipeOffset = (swipeOffset + dragAmount).coerceIn(0f, maxSwipePx)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Swipe to delete",
                tint = Color.White
            )
        }
    }
}
"""

content += swipe_to_confirm_code

with open('/app/applet/app/src/main/java/com/example/ui/screens/LaborDetailScreen.kt', 'w') as f:
    f.write(content)
