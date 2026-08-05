package com.example.ui.remote.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.remote.RemoteCommand

@Composable
fun NumericKeypad(
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val keys = listOf(
        listOf(
            Triple("1", RemoteCommand.KEY_1, "key_1"),
            Triple("2", RemoteCommand.KEY_2, "key_2"),
            Triple("3", RemoteCommand.KEY_3, "key_3")
        ),
        listOf(
            Triple("4", RemoteCommand.KEY_4, "key_4"),
            Triple("5", RemoteCommand.KEY_5, "key_5"),
            Triple("6", RemoteCommand.KEY_6, "key_6")
        ),
        listOf(
            Triple("7", RemoteCommand.KEY_7, "key_7"),
            Triple("8", RemoteCommand.KEY_8, "key_8"),
            Triple("9", RemoteCommand.KEY_9, "key_9")
        ),
        listOf(
            Triple("INFO", RemoteCommand.INFO, "key_info"),
            Triple("0", RemoteCommand.KEY_0, "key_0"),
            Triple("GUIDE", RemoteCommand.GUIDE, "key_guide")
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (row in keys) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for ((label, command, tag) in row) {
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCommand(command)
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag(tag)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = if (label.length > 2) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
