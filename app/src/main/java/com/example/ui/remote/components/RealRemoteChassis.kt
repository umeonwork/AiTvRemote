package com.example.ui.remote.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.remote.BrandType
import com.example.ui.remote.RemoteCommand
import kotlinx.coroutines.delay

@Composable
fun RealRemoteChassis(
    brandType: BrandType,
    hasIrEmitter: Boolean,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    var isTransmitting by remember { mutableStateOf(false) }
    var lastCommandName by remember { mutableStateOf<String?>(null) }

    val handleCommand: (RemoteCommand) -> Unit = { command ->
        isTransmitting = true
        lastCommandName = command.name.replace("_", " ")
        onCommand(command)
    }

    LaunchedEffect(isTransmitting) {
        if (isTransmitting) {
            delay(350)
            isTransmitting = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        // Transmitting Flash Banner
        AnimatedVisibility(
            visible = isTransmitting,
            enter = fadeIn(animationSpec = tween(100)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Surface(
                color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transmitting ${lastCommandName ?: ""}...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // PHYSICAL REMOTE CASING BODY
        Box(
            modifier = Modifier
                .width(320.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = getRemoteShape(brandType),
                    clip = false
                )
                .clip(getRemoteShape(brandType))
                .background(getRemoteBackgroundBrush(brandType))
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    ),
                    shape = getRemoteShape(brandType)
                )
                .padding(vertical = 20.dp, horizontal = 18.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TOP LENS / IR EMITTER BULB
                IrEmitterBulb(isTransmitting = isTransmitting, hasIr = hasIrEmitter)

                // BRAND SPECIFIC BODY LAYOUT
                when (brandType) {
                    BrandType.SAMSUNG -> SamsungRemoteBody(onCommand = handleCommand)
                    BrandType.LG -> LgRemoteBody(onCommand = handleCommand)
                    BrandType.SONY -> SonyRemoteBody(onCommand = handleCommand)
                    BrandType.XIAOMI -> XiaomiRemoteBody(onCommand = handleCommand)
                    BrandType.ONEPLUS -> OnePlusRemoteBody(onCommand = handleCommand)
                    else -> UniversalRemoteBody(brandType = brandType, onCommand = handleCommand)
                }
            }
        }
    }
}

@Composable
private fun IrEmitterBulb(isTransmitting: Boolean, hasIr: Boolean) {
    val emitterGlowColor = if (isTransmitting) Color(0xFF00E5FF) else if (hasIr) Color(0xFF4CAF50) else Color(0xFFFF9800)
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(Color(0xFF101216)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (isTransmitting) 10.dp else 6.dp)
                    .clip(CircleShape)
                    .background(emitterGlowColor)
                    .shadow(if (isTransmitting) 12.dp else 2.dp, CircleShape, spotColor = emitterGlowColor)
            )
        }
    }
}

// ==========================================
// 1. SAMSUNG SMART WAND BODY
// ==========================================
@Composable
private fun SamsungRemoteBody(onCommand: (RemoteCommand) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Power & Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Realistic3DButton(
                icon = Icons.Default.PowerSettingsNew,
                contentDescription = "Samsung Power",
                onClick = { onCommand(RemoteCommand.POWER) },
                buttonColor = Color(0xFFE53935),
                size = 46.dp
            )
            Text(
                text = "SAMSUNG",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 3.sp
                ),
                fontWeight = FontWeight.Black,
                color = Color(0xFFE0E6ED)
            )
            Realistic3DButton(
                icon = Icons.Default.Input,
                contentDescription = "Input Source",
                onClick = { onCommand(RemoteCommand.INPUT) },
                buttonColor = Color(0xFF374151),
                size = 46.dp
            )
        }

        // Silver Circular D-Pad Wheel
        RealisticDPadWheel(
            onCommand = onCommand,
            outerRingColor = Color(0xFF4B5563),
            innerButtonColor = Color(0xFF6B7280)
        )

        // Nav Buttons (Back, Home, Play/Pause)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Realistic3DButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = { onCommand(RemoteCommand.BACK) },
                buttonColor = Color(0xFF374151)
            )
            Realistic3DButton(
                icon = Icons.Default.Home,
                contentDescription = "Home",
                onClick = { onCommand(RemoteCommand.HOME) },
                buttonColor = Color(0xFF2563EB)
            )
            Realistic3DButton(
                icon = Icons.Default.PlayArrow,
                contentDescription = "Play Pause",
                onClick = { onCommand(RemoteCommand.PLAY) },
                buttonColor = Color(0xFF374151)
            )
        }

        // Dual Metallic Vertical Rockers (Volume & Channel)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            RealisticRockerSwitch(
                label = "VOL",
                onUp = { onCommand(RemoteCommand.VOL_UP) },
                onDown = { onCommand(RemoteCommand.VOL_DOWN) },
                onCenterClick = { onCommand(RemoteCommand.MUTE) }
            )
            RealisticRockerSwitch(
                label = "CH",
                onUp = { onCommand(RemoteCommand.CH_UP) },
                onDown = { onCommand(RemoteCommand.CH_DOWN) },
                onCenterClick = { onCommand(RemoteCommand.GUIDE) }
            )
        }

        // OTT App Buttons (Netflix, Prime, Hotstar, Samsung TV Plus)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OttAppPillButton("NETFLIX", Color(0xFFE50914), Color.White) { onCommand(RemoteCommand.APP_NETFLIX) }
            OttAppPillButton("prime", Color(0xFF00A8E1), Color.White) { onCommand(RemoteCommand.APP_PRIME) }
            OttAppPillButton("Hotstar", Color(0xFF0C2040), Color.White) { onCommand(RemoteCommand.APP_HOTSTAR) }
        }
    }
}

// ==========================================
// 2. LG MAGIC REMOTE BODY
// ==========================================
@Composable
private fun LgRemoteBody(onCommand: (RemoteCommand) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Realistic3DButton(
                icon = Icons.Default.PowerSettingsNew,
                contentDescription = "LG Power",
                onClick = { onCommand(RemoteCommand.POWER) },
                buttonColor = Color(0xFFD32F2F),
                size = 46.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LG ThinQ",
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "webOS Magic Remote",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA0AEC0)
                )
            }
            Realistic3DButton(
                icon = Icons.Default.Input,
                contentDescription = "Input Source",
                onClick = { onCommand(RemoteCommand.INPUT) },
                buttonColor = Color(0xFF2D3748),
                size = 46.dp
            )
        }

        // 4 Color Keys Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ColorCircleButton(Color(0xFFE53935)) { onCommand(RemoteCommand.RED_BUTTON) }
            ColorCircleButton(Color(0xFF43A047)) { onCommand(RemoteCommand.GREEN_BUTTON) }
            ColorCircleButton(Color(0xFFFDD835)) { onCommand(RemoteCommand.YELLOW_BUTTON) }
            ColorCircleButton(Color(0xFF1E88E5)) { onCommand(RemoteCommand.BLUE_BUTTON) }
        }

        // LG Magic Wheel D-Pad (with Scroll Wheel Center)
        RealisticDPadWheel(
            onCommand = onCommand,
            outerRingColor = Color(0xFF2D3748),
            innerButtonColor = Color(0xFFC0C0C0),
            centerLabel = "WHEEL"
        )

        // Magic Pointer & Home Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Realistic3DButton(
                icon = Icons.Default.AdsClick,
                contentDescription = "Magic Pointer",
                onClick = { onCommand(RemoteCommand.LG_POINTER) },
                buttonColor = Color(0xFF805AD5)
            )
            Realistic3DButton(
                icon = Icons.Default.Home,
                contentDescription = "Smart Home",
                onClick = { onCommand(RemoteCommand.HOME) },
                buttonColor = Color(0xFF3182CE)
            )
            Realistic3DButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = { onCommand(RemoteCommand.BACK) },
                buttonColor = Color(0xFF2D3748)
            )
        }

        // Volume & Channel Rockers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            RealisticRockerSwitch("VOL", { onCommand(RemoteCommand.VOL_UP) }, { onCommand(RemoteCommand.VOL_DOWN) }, { onCommand(RemoteCommand.MUTE) })
            RealisticRockerSwitch("CH", { onCommand(RemoteCommand.CH_UP) }, { onCommand(RemoteCommand.CH_DOWN) }, { onCommand(RemoteCommand.GUIDE) })
        }

        // OTT Shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OttAppPillButton("NETFLIX", Color(0xFFE50914), Color.White) { onCommand(RemoteCommand.APP_NETFLIX) }
            OttAppPillButton("Disney+", Color(0xFF00142A), Color.White) { onCommand(RemoteCommand.APP_HOTSTAR) }
            OttAppPillButton("YouTube", Color(0xFFFF0000), Color.White) { onCommand(RemoteCommand.APP_YOUTUBE) }
        }
    }
}

// ==========================================
// 3. SONY BRAVIA REMOTE BODY
// ==========================================
@Composable
private fun SonyRemoteBody(onCommand: (RemoteCommand) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121417), RoundedCornerShape(8.dp))
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SONY",
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 4.sp),
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        // Power / Input / TV
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Realistic3DButton(Icons.Default.PowerSettingsNew, "Power", { onCommand(RemoteCommand.POWER) }, buttonColor = Color(0xFFE53935))
            Realistic3DButton(Icons.Default.Input, "Input", { onCommand(RemoteCommand.INPUT) }, buttonColor = Color(0xFF333A42))
            Realistic3DButton(Icons.Default.Tv, "TV / Guide", { onCommand(RemoteCommand.GUIDE) }, buttonColor = Color(0xFF333A42))
        }

        // 4 Color Keys
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ColorCircleButton(Color(0xFFE53935)) { onCommand(RemoteCommand.RED_BUTTON) }
            ColorCircleButton(Color(0xFF43A047)) { onCommand(RemoteCommand.GREEN_BUTTON) }
            ColorCircleButton(Color(0xFFFDD835)) { onCommand(RemoteCommand.YELLOW_BUTTON) }
            ColorCircleButton(Color(0xFF1E88E5)) { onCommand(RemoteCommand.BLUE_BUTTON) }
        }

        // Action Menu / Sync Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = { onCommand(RemoteCommand.SONY_ACTION_MENU) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
            ) {
                Text("ACTION MENU", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = { onCommand(RemoteCommand.SONY_SYNC_MENU) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
            ) {
                Text("APPS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        // D-Pad
        RealisticDPadWheel(onCommand = onCommand, outerRingColor = Color(0xFF2A2E37), innerButtonColor = Color(0xFF3F4452))

        // Back / Home / Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Realistic3DButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", { onCommand(RemoteCommand.BACK) }, buttonColor = Color(0xFF333A42))
            Realistic3DButton(Icons.Default.Home, "Home", { onCommand(RemoteCommand.HOME) }, buttonColor = Color(0xFF2563EB))
            Realistic3DButton(Icons.Default.Menu, "Menu", { onCommand(RemoteCommand.MENU) }, buttonColor = Color(0xFF333A42))
        }

        // Rockers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            RealisticRockerSwitch("VOL", { onCommand(RemoteCommand.VOL_UP) }, { onCommand(RemoteCommand.VOL_DOWN) }, { onCommand(RemoteCommand.MUTE) })
            RealisticRockerSwitch("CH", { onCommand(RemoteCommand.CH_UP) }, { onCommand(RemoteCommand.CH_DOWN) }, { onCommand(RemoteCommand.GUIDE) })
        }

        // Sony OTT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OttAppPillButton("YouTube", Color(0xFFFF0000), Color.White) { onCommand(RemoteCommand.APP_YOUTUBE) }
            OttAppPillButton("NETFLIX", Color(0xFFE50914), Color.White) { onCommand(RemoteCommand.APP_NETFLIX) }
            OttAppPillButton("SonyLIV", Color(0xFF0F172A), Color.White) { onCommand(RemoteCommand.APP_SONYLIV) }
        }
    }
}

// ==========================================
// 4. XIAOMI / REDMI SMART REMOTE BODY
// ==========================================
@Composable
private fun XiaomiRemoteBody(onCommand: (RemoteCommand) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Realistic3DButton(Icons.Default.PowerSettingsNew, "Power", { onCommand(RemoteCommand.POWER) }, buttonColor = Color(0xFFE53935))
            
            // Google Assistant Voice Dot Button
            Surface(
                onClick = { onCommand(RemoteCommand.SELECT) },
                shape = CircleShape,
                color = Color(0xFF22252B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F4452)),
                modifier = Modifier.size(44.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4285F4)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEA4335)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFBBC05)))
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF34A853)))
                }
            }

            Realistic3DButton(Icons.Default.VolumeOff, "Mute", { onCommand(RemoteCommand.MUTE) }, buttonColor = Color(0xFF333A42))
        }

        // Circular D-Pad
        RealisticDPadWheel(onCommand = onCommand, outerRingColor = Color(0xFF2B2E36), innerButtonColor = Color(0xFF3A3E4A))

        // PatchWall / Android Home / Back
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Surface(
                onClick = { onCommand(RemoteCommand.XIAOMI_PATCHWALL) },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFF6600),
                modifier = Modifier.height(40.dp).width(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("PatchWall", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Realistic3DButton(Icons.Default.Home, "Home", { onCommand(RemoteCommand.HOME) }, buttonColor = Color(0xFF2563EB))
            Realistic3DButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", { onCommand(RemoteCommand.BACK) }, buttonColor = Color(0xFF333A42))
        }

        // Volume Rocker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            RealisticRockerSwitch("VOL", { onCommand(RemoteCommand.VOL_UP) }, { onCommand(RemoteCommand.VOL_DOWN) }, { onCommand(RemoteCommand.MUTE) })
        }

        // OTT Apps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OttAppPillButton("NETFLIX", Color(0xFFE50914), Color.White) { onCommand(RemoteCommand.APP_NETFLIX) }
            OttAppPillButton("prime", Color(0xFF00A8E1), Color.White) { onCommand(RemoteCommand.APP_PRIME) }
            OttAppPillButton("JioCinema", Color(0xFFD81B60), Color.White) { onCommand(RemoteCommand.APP_JIOCINEMA) }
        }

        // Bottom Orange "mi" emblem
        Text(
            text = "mi",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFF6600)
        )
    }
}

// ==========================================
// 5. ONEPLUS REMOTE BODY
// ==========================================
@Composable
private fun OnePlusRemoteBody(onCommand: (RemoteCommand) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Realistic3DButton(Icons.Default.PowerSettingsNew, "Power", { onCommand(RemoteCommand.POWER) }, buttonColor = Color(0xFFEB0029))
            Text("ONEPLUS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White)
            Realistic3DButton(Icons.Default.Input, "Input", { onCommand(RemoteCommand.INPUT) }, buttonColor = Color(0xFF2B2E36))
        }

        // D-Pad with Red Center Dot
        RealisticDPadWheel(onCommand = onCommand, outerRingColor = Color(0xFF232730), innerButtonColor = Color(0xFFEB0029))

        // OxygenPlay / Home / Back
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Surface(
                onClick = { onCommand(RemoteCommand.ONEPLUS_OXYGENPLAY) },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEB0029),
                modifier = Modifier.height(40.dp).width(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("OxygenPlay", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Realistic3DButton(Icons.Default.Home, "Home", { onCommand(RemoteCommand.HOME) }, buttonColor = Color(0xFF2563EB))
            Realistic3DButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", { onCommand(RemoteCommand.BACK) }, buttonColor = Color(0xFF2B2E36))
        }

        // Volume Rocker
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            RealisticRockerSwitch("VOL", { onCommand(RemoteCommand.VOL_UP) }, { onCommand(RemoteCommand.VOL_DOWN) }, { onCommand(RemoteCommand.MUTE) })
        }

        // App Shortcuts
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OttAppPillButton("YouTube", Color(0xFFFF0000), Color.White) { onCommand(RemoteCommand.APP_YOUTUBE) }
            OttAppPillButton("Hotstar", Color(0xFF0C2040), Color.White) { onCommand(RemoteCommand.APP_HOTSTAR) }
            OttAppPillButton("ZEE5", Color(0xFF8257E5), Color.White) { onCommand(RemoteCommand.APP_ZEE5) }
        }
    }
}

// ==========================================
// 6. UNIVERSAL / ANDROID TV / OTHER BRANDS BODY
// ==========================================
@Composable
private fun UniversalRemoteBody(brandType: BrandType, onCommand: (RemoteCommand) -> Unit) {
    var showNumericPad by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Realistic3DButton(Icons.Default.PowerSettingsNew, "Power", { onCommand(RemoteCommand.POWER) }, buttonColor = Color(0xFFD32F2F))
            Text(
                text = brandType.displayName.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Realistic3DButton(Icons.Default.Input, "Input", { onCommand(RemoteCommand.INPUT) }, buttonColor = Color(0xFF333A42))
        }

        // Quick Mode Toggle (D-Pad vs Number Grid)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E222A), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = !showNumericPad,
                onClick = { showNumericPad = false },
                label = { Text("Navigation D-Pad") }
            )
            FilterChip(
                selected = showNumericPad,
                onClick = { showNumericPad = true },
                label = { Text("0-9 Numbers") }
            )
        }

        if (showNumericPad) {
            // 0-9 Keypad Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    listOf(RemoteCommand.KEY_1, RemoteCommand.KEY_2, RemoteCommand.KEY_3),
                    listOf(RemoteCommand.KEY_4, RemoteCommand.KEY_5, RemoteCommand.KEY_6),
                    listOf(RemoteCommand.KEY_7, RemoteCommand.KEY_8, RemoteCommand.KEY_9)
                ).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { cmd ->
                            Realistic3DButton(
                                icon = null,
                                contentDescription = cmd.name,
                                onClick = { onCommand(cmd) },
                                buttonColor = Color(0xFF2A2E37),
                                label = cmd.name.replace("KEY_", ""),
                                size = 48.dp
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Realistic3DButton(
                        icon = null,
                        contentDescription = "KEY_0",
                        onClick = { onCommand(RemoteCommand.KEY_0) },
                        buttonColor = Color(0xFF2A2E37),
                        label = "0",
                        size = 48.dp
                    )
                }
            }
        } else {
            // D-Pad Wheel
            RealisticDPadWheel(onCommand = onCommand, outerRingColor = Color(0xFF282C35), innerButtonColor = Color(0xFF3B404E))
        }

        // Nav Keys
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Realistic3DButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", { onCommand(RemoteCommand.BACK) }, buttonColor = Color(0xFF333A42))
            Realistic3DButton(Icons.Default.Home, "Home", { onCommand(RemoteCommand.HOME) }, buttonColor = Color(0xFF2563EB))
            Realistic3DButton(Icons.Default.Menu, "Menu", { onCommand(RemoteCommand.MENU) }, buttonColor = Color(0xFF333A42))
        }

        // Volume / Channel Rockers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            RealisticRockerSwitch("VOL", { onCommand(RemoteCommand.VOL_UP) }, { onCommand(RemoteCommand.VOL_DOWN) }, { onCommand(RemoteCommand.MUTE) })
            RealisticRockerSwitch("CH", { onCommand(RemoteCommand.CH_UP) }, { onCommand(RemoteCommand.CH_DOWN) }, { onCommand(RemoteCommand.GUIDE) })
        }

        // OTT App Shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OttAppPillButton("NETFLIX", Color(0xFFE50914), Color.White) { onCommand(RemoteCommand.APP_NETFLIX) }
            OttAppPillButton("Prime", Color(0xFF00A8E1), Color.White) { onCommand(RemoteCommand.APP_PRIME) }
            OttAppPillButton("YouTube", Color(0xFFFF0000), Color.White) { onCommand(RemoteCommand.APP_YOUTUBE) }
        }
    }
}

// ==========================================
// SHARED REALISTIC COMPONENT BUILDING BLOCKS
// ==========================================

@Composable
fun Realistic3DButton(
    icon: ImageVector?,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonColor: Color = Color(0xFF2A2E37),
    contentColor: Color = Color.White,
    size: Dp = 48.dp,
    label: String? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 80), label = "press_scale"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        interactionSource = interactionSource,
        shape = CircleShape,
        color = buttonColor,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = if (isPressed) 0.5f else 0.15f)
        ),
        shadowElevation = if (isPressed) 1.dp else 4.dp,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag("btn_${contentDescription.lowercase().replace(" ", "_")}")
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = contentColor,
                    modifier = Modifier.size(size * 0.5f)
                )
            } else if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun ColorCircleButton(color: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = CircleShape,
        color = color,
        shadowElevation = 3.dp,
        modifier = Modifier.size(28.dp)
    ) {}
}

@Composable
fun OttAppPillButton(
    title: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Button(
        onClick = {
            try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Throwable) {}
            onClick()
        },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        modifier = Modifier
            .height(38.dp)
            .width(88.dp)
            .testTag("ott_${title.lowercase()}")
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun RealisticDPadWheel(
    onCommand: (RemoteCommand) -> Unit,
    outerRingColor: Color = Color(0xFF282C35),
    innerButtonColor: Color = Color(0xFF3B404E),
    centerLabel: String = "OK"
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(210.dp)
            .clip(CircleShape)
            .background(outerRingColor)
            .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .shadow(6.dp, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // UP
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.UP)
            },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).size(52.dp).testTag("dpad_up")
        ) {
            Icon(Icons.Default.KeyboardArrowUp, "Up", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        // DOWN
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.DOWN)
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).size(52.dp).testTag("dpad_down")
        ) {
            Icon(Icons.Default.KeyboardArrowDown, "Down", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        // LEFT
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.LEFT)
            },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(52.dp).testTag("dpad_left")
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        // RIGHT
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.RIGHT)
            },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(52.dp).testTag("dpad_right")
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        // CENTER OK BUTTON
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCommand(RemoteCommand.SELECT)
            },
            shape = CircleShape,
            color = innerButtonColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            shadowElevation = 6.dp,
            modifier = Modifier.size(80.dp).testTag("dpad_select")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun RealisticRockerSwitch(
    label: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onCenterClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF232730),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 6.dp,
        modifier = Modifier
            .width(54.dp)
            .height(110.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // UP (+)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUp()
                },
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("rocker_${label.lowercase()}_up")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, "+", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // LABEL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181A20))
                    .padding(vertical = 2.dp)
                    .clickable(enabled = onCenterClick != null) {
                        onCenterClick?.invoke()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
            }

            // DOWN (-)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDown()
                },
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("rocker_${label.lowercase()}_down")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Remove, "-", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// Helpers for Remote Casing Shape & Brushes
private fun getRemoteShape(brandType: BrandType): RoundedCornerShape {
    return when (brandType) {
        BrandType.SAMSUNG -> RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp, bottomStart = 44.dp, bottomEnd = 44.dp)
        BrandType.LG -> RoundedCornerShape(topStart = 42.dp, topEnd = 42.dp, bottomStart = 50.dp, bottomEnd = 50.dp)
        BrandType.XIAOMI, BrandType.ONEPLUS -> RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 36.dp, bottomEnd = 36.dp)
        else -> RoundedCornerShape(28.dp)
    }
}

private fun getRemoteBackgroundBrush(brandType: BrandType): Brush {
    return when (brandType) {
        BrandType.SAMSUNG -> Brush.verticalGradient(listOf(Color(0xFF282C34), Color(0xFF1B1D23), Color(0xFF14161B)))
        BrandType.LG -> Brush.verticalGradient(listOf(Color(0xFF1E2026), Color(0xFF13151A), Color(0xFF0D0E12)))
        BrandType.SONY -> Brush.verticalGradient(listOf(Color(0xFF20232A), Color(0xFF17191E), Color(0xFF111216)))
        BrandType.XIAOMI -> Brush.verticalGradient(listOf(Color(0xFF262930), Color(0xFF1A1D23), Color(0xFF121418)))
        BrandType.ONEPLUS -> Brush.verticalGradient(listOf(Color(0xFF2A2D35), Color(0xFF1C1E24), Color(0xFF131418)))
        else -> Brush.verticalGradient(listOf(Color(0xFF2B2E36), Color(0xFF1E2026), Color(0xFF15171C)))
    }
}
