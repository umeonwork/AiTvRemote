package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.example.ui.remote.RemoteCommand
import com.example.viewmodel.MainViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchpadScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val commandStatusMessage by viewModel.commandStatusMessage.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(commandStatusMessage) {
        commandStatusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedDevice?.let { "Touchpad (${it.name})" } ?: "Smart Touchpad") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                "Swipe left/right/up/down to navigate, tap to select/OK",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                totalDragX = 0f
                                totalDragY = 0f
                            },
                            onDragEnd = {
                                val threshold = 40f
                                if (abs(totalDragX) > abs(totalDragY)) {
                                    if (totalDragX > threshold) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.sendCommand(RemoteCommand.RIGHT)
                                    } else if (totalDragX < -threshold) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.sendCommand(RemoteCommand.LEFT)
                                    }
                                } else {
                                    if (totalDragY > threshold) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.sendCommand(RemoteCommand.DOWN)
                                    } else if (totalDragY < -threshold) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.sendCommand(RemoteCommand.UP)
                                    }
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.sendCommand(RemoteCommand.SELECT)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Touchpad Canvas",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Swipe directional gestures or tap center OK",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation bar controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.sendCommand(RemoteCommand.BACK)
                    }
                ) {
                    Text("TV Back")
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.sendCommand(RemoteCommand.SELECT)
                    }
                ) {
                    Text("OK / Select")
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.sendCommand(RemoteCommand.HOME)
                    }
                ) {
                    Text("TV Home")
                }
            }
        }
    }
}
