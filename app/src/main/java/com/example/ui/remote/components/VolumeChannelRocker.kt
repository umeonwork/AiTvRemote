package com.example.ui.remote.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.remote.RemoteCommand

@Composable
fun VolumeChannelRocker(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Volume Rocker
        RockerColumn(
            label = "VOL",
            onUpClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.VOL_UP)
            },
            onDownClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.VOL_DOWN)
            },
            upIcon = Icons.Default.Add,
            downIcon = Icons.Default.Remove,
            upTestTag = "vol_up_btn",
            downTestTag = "vol_down_btn"
        )

        // Channel Rocker
        RockerColumn(
            label = "CH",
            onUpClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.CH_UP)
            },
            onDownClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.CH_DOWN)
            },
            upIcon = Icons.Default.KeyboardArrowUp,
            downIcon = Icons.Default.KeyboardArrowDown,
            upTestTag = "ch_up_btn",
            downTestTag = "ch_down_btn"
        )
    }
}

@Composable
private fun RockerColumn(
    label: String,
    onUpClick: () -> Unit,
    onDownClick: () -> Unit,
    upIcon: androidx.compose.ui.graphics.vector.ImageVector,
    downIcon: androidx.compose.ui.graphics.vector.ImageVector,
    upTestTag: String,
    downTestTag: String
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
        modifier = Modifier.width(68.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            IconButton(
                onClick = onUpClick,
                modifier = Modifier
                    .size(52.dp)
                    .testTag(upTestTag)
            ) {
                Icon(
                    imageVector = upIcon,
                    contentDescription = "$label Up",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            IconButton(
                onClick = onDownClick,
                modifier = Modifier
                    .size(52.dp)
                    .testTag(downTestTag)
            ) {
                Icon(
                    imageVector = downIcon,
                    contentDescription = "$label Down",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
