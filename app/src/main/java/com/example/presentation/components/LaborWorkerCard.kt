package com.example.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.LaborWorker
import com.example.presentation.theme.LaborBlue
import com.example.presentation.theme.LaborError
import com.example.presentation.theme.LaborSuccess
import com.example.presentation.theme.LaborTextPrimary
import com.example.presentation.theme.LaborTextSecondary

private val WorkerCardShape = RoundedCornerShape(12.dp)
private val WorkerBorderStroke = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
private val AvatarBorderColor = Color(0xFFE5E7EB)

@Composable
fun LaborWorkerCard(
    worker: LaborWorker,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    showDivider: Boolean = false
) {
    val initial = remember(worker.name) { worker.name.take(1).uppercase() }

    val avatarBgColor = remember(worker.id) {
        val colors = listOf(
            Color(0xFFE3F2FD), // Light Blue
            Color(0xFFF3E5F5), // Light Purple
            Color(0xFFE8F5E9), // Light Green
            Color(0xFFFFF3E0), // Light Orange
            Color(0xFFFFEBEE), // Light Red
            Color(0xFFE0F7FA), // Light Cyan
            Color(0xFFFCE4EC), // Light Pink
            Color(0xFFF1F8E9), // Light Lime
            Color(0xFFFFF8E1), // Light Amber
            Color(0xFFEDE7F6)  // Light Deep Purple
        )
        val hash = worker.id.hashCode()
        colors[kotlin.math.abs(hash) % colors.size]
    }

    Surface(
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("labor_worker_card_${worker.id}"),
        shape = shape,
        color = Color.White
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(avatarBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
                    
                Spacer(modifier = Modifier.width(12.dp))
                    
                // Name and Phone
                Column {
                    Text(
                        text = worker.name,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (worker.phoneNumber.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = worker.phoneNumber,
                            fontSize = 13.sp,
                            color = Color(0xFF4B5563)
                        )
                    }
                }
            }
            if (showDivider) {
                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}


@Composable
fun SkillPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatusChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = 1.5.dp,
                color = color,
                shape = RoundedCornerShape(6.dp)
            )
            .background(if (isSelected) color else Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else color
        )
    }
}
