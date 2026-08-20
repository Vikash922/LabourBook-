package com.example.presentation.components
import androidx.compose.foundation.layout.navigationBarsPadding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.theme.LaborBlue
import com.example.presentation.theme.LaborTextSecondary
import com.example.core.util.AppStrings

@Composable
fun LaborbookBottomNav(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    language: String = "English",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().navigationBarsPadding(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tab Items Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Labor
                BottomNavItem(
                    title = AppStrings.get("labor", language),
                    icon = Icons.Outlined.People,
                    isSelected = selectedTabIndex == 0,
                    testTag = "nav_tab_labor",
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.weight(1f)
                )

                // Tab 1: Cash book
                BottomNavItem(
                    title = AppStrings.get("cash_book", language),
                    icon = Icons.Outlined.Receipt,
                    isSelected = selectedTabIndex == 1,
                    testTag = "nav_tab_cashbook",
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.weight(1f)
                )

                // Tab 2: Settings
                BottomNavItem(
                    title = AppStrings.get("settings", language),
                    icon = Icons.Outlined.Settings,
                    isSelected = selectedTabIndex == 2,
                    testTag = "nav_tab_settings",
                    onClick = { onTabSelected(2) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Indicator Bar Line below the tab row
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (selectedTabIndex == 0) {
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(50.dp)
                                .background(LaborBlue)
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (selectedTabIndex == 1) {
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(50.dp)
                                .background(LaborBlue)
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (selectedTabIndex == 2) {
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(50.dp)
                                .background(LaborBlue)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) LaborBlue else LaborTextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) LaborBlue else LaborTextSecondary
        )
    }
}
