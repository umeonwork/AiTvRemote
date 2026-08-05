package com.example.ui.remote.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.remote.BrandType
import com.example.ui.remote.RemoteCommand

@Composable
fun BrandCustomControls(
    brandType: BrandType,
    showColorButtons: Boolean,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Color Buttons (Red, Green, Yellow, Blue) if supported
        if (showColorButtons) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ColorButton(color = Color(0xFFE53935), tag = "color_red") {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCommand(RemoteCommand.RED_BUTTON)
                }
                ColorButton(color = Color(0xFF43A047), tag = "color_green") {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCommand(RemoteCommand.GREEN_BUTTON)
                }
                ColorButton(color = Color(0xFFFDD835), tag = "color_yellow") {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCommand(RemoteCommand.YELLOW_BUTTON)
                }
                ColorButton(color = Color(0xFF1E88E5), tag = "color_blue") {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCommand(RemoteCommand.BLUE_BUTTON)
                }
            }
        }

        // Brand-specific special action row
        when (brandType) {
            BrandType.XIAOMI -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onCommand(RemoteCommand.XIAOMI_PATCHWALL) },
                        modifier = Modifier.testTag("xiaomi_patchwall_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6700))
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PatchWall")
                    }
                    OutlinedButton(
                        onClick = { onCommand(RemoteCommand.HOME) },
                        modifier = Modifier.testTag("xiaomi_android_home_btn")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Android Home")
                    }
                }
            }
            BrandType.ONEPLUS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onCommand(RemoteCommand.ONEPLUS_OXYGENPLAY) },
                        modifier = Modifier.testTag("oneplus_oxygen_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB0029))
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OxygenPlay")
                    }
                    OutlinedButton(onClick = { onCommand(RemoteCommand.INPUT) }) {
                        Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("HDMI Source")
                    }
                }
            }
            BrandType.HISENSE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onCommand(RemoteCommand.HISENSE_VIDAA_HOME) },
                        modifier = Modifier.testTag("hisense_vidaa_btn")
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VIDAA Home")
                    }
                    OutlinedButton(onClick = { onCommand(RemoteCommand.PLAY) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quick Play")
                    }
                }
            }
            BrandType.LG -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { onCommand(RemoteCommand.LG_POINTER) },
                        modifier = Modifier.testTag("lg_pointer_btn")
                    ) {
                        Icon(Icons.Default.AdsClick, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Magic Pointer")
                    }
                    OutlinedButton(
                        onClick = { onCommand(RemoteCommand.LG_SMART_HOME) },
                        modifier = Modifier.testTag("lg_smart_home_btn")
                    ) {
                        Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Smart Dashboard")
                    }
                }
            }
            BrandType.SAMSUNG -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { onCommand(RemoteCommand.SAMSUNG_AMBIENT) },
                        modifier = Modifier.testTag("samsung_ambient_btn")
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ambient Mode")
                    }
                    OutlinedButton(
                        onClick = { onCommand(RemoteCommand.SAMSUNG_SOURCE) },
                        modifier = Modifier.testTag("samsung_source_btn")
                    ) {
                        Icon(Icons.Default.Input, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Source Select")
                    }
                }
            }
            BrandType.SONY -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { onCommand(RemoteCommand.SONY_SYNC_MENU) },
                        modifier = Modifier.testTag("sony_sync_btn")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Menu")
                    }
                    OutlinedButton(
                        onClick = { onCommand(RemoteCommand.SONY_ACTION_MENU) },
                        modifier = Modifier.testTag("sony_action_btn")
                    ) {
                        Icon(Icons.Default.DisplaySettings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Action Menu")
                    }
                }
            }
            else -> {
                // Default universal quick actions (Guide / Info)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { onCommand(RemoteCommand.GUIDE) }) {
                        Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guide")
                    }
                    OutlinedButton(onClick = { onCommand(RemoteCommand.INFO) }) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Info")
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorButton(
    color: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color,
        modifier = Modifier
            .size(36.dp)
            .testTag(tag)
    ) {
        Box(modifier = Modifier.fillMaxSize())
    }
}
