package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceEntity

/**
 * Status indicator component that displays the current app connection state:
 * - Connected to a TV device
 * - Currently searching/scanning for TVs on Wi-Fi
 * - Disconnected or IR Blaster ready state
 */
@Composable
fun ConnectionStatusIndicator(
    selectedDevice: DeviceEntity?,
    isScanning: Boolean,
    hasIrEmitter: Boolean = false,
    onScanClick: (() -> Unit)? = null,
    onSelectDeviceClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Pulse animation for searching state or active connection
    val infiniteTransition = rememberInfiniteTransition(label = "StatusPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val containerColor = when {
        isScanning -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        selectedDevice != null -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val borderColor = when {
        isScanning -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
        selectedDevice != null -> Color(0xFF4CAF50).copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .testTag("connection_status_indicator"),
        color = containerColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Status Icon & Pulse Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp)
            ) {
                if (isScanning || selectedDevice != null) {
                    val pulseColor = if (isScanning) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        Color(0xFF4CAF50)
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .scale(if (isScanning) pulseScale else 1.0f)
                            .alpha(if (isScanning) pulseAlpha else 0.25f)
                            .clip(CircleShape)
                            .background(pulseColor)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = when {
                        isScanning -> MaterialTheme.colorScheme.tertiary
                        selectedDevice != null -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onTertiary,
                                strokeWidth = 2.dp
                            )
                        } else if (selectedDevice != null) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = "Connected TV",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.TvOff,
                                contentDescription = "Disconnected",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center Status Info Text
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = onSelectDeviceClick != null) {
                        onSelectDeviceClick?.invoke()
                    }
            ) {
                Crossfade(
                    targetState = Triple(selectedDevice, isScanning, hasIrEmitter),
                    label = "StatusTextCrossfade"
                ) { (device, scanning, ir) ->
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = when {
                                    scanning -> "Searching for TVs..."
                                    device != null -> device.name
                                    else -> "No TV Connected"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("status_text")
                            )

                            // Status Badge Chip
                            val (badgeText, badgeColor, badgeTextColor) = when {
                                scanning -> Triple(
                                    "SEARCHING",
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                device != null -> Triple(
                                    "CONNECTED",
                                    Color(0xFFE8F5E9),
                                    Color(0xFF2E7D32)
                                )
                                else -> Triple(
                                    if (ir) "IR READY" else "OFFLINE",
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor,
                                modifier = Modifier.testTag("status_badge")
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = when {
                                scanning -> "Scanning local Wi-Fi network via NSD / mDNS"
                                device != null -> {
                                    val ipStr = device.ipAddress.ifEmpty { "Network" }
                                    val typeStr = device.type ?: "Smart TV"
                                    "$typeStr • $ipStr ${if (ir) "• IR Mode Active" else ""}"
                                }
                                ir -> "IR Blaster detected on phone. Tap to scan for Wi-Fi TVs."
                                else -> "Tap to scan Wi-Fi network or add TV manually"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right Action Button (Scan / Change TV)
            if (onScanClick != null || onSelectDeviceClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (isScanning) {
                            onSelectDeviceClick?.invoke()
                        } else if (onScanClick != null) {
                            onScanClick.invoke()
                        } else {
                            onSelectDeviceClick?.invoke()
                        }
                    },
                    modifier = Modifier.testTag("status_action_btn")
                ) {
                    if (isScanning) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Searching",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    } else if (selectedDevice != null) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan TVs",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Scan Network",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
