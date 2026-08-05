package com.example.ui.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DeviceEntity
import com.example.ui.components.ConnectionStatusIndicator
import com.example.ui.remote.components.RealRemoteChassis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseRemoteLayout(
    device: DeviceEntity?,
    hasIrEmitter: Boolean = false,
    isScanning: Boolean = false,
    onScanClick: (() -> Unit)? = null,
    onTestConnectionClick: (() -> Unit)? = null,
    onCommand: (RemoteCommand) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToTouchpad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detectedBrand = BrandType.fromDeviceType(device?.type ?: device?.name)
    var selectedBrandSkin by remember(detectedBrand) { mutableStateOf(detectedBrand) }
    val haptic = LocalHapticFeedback.current

    val featuredBrands = listOf(
        BrandType.SAMSUNG,
        BrandType.LG,
        BrandType.SONY,
        BrandType.XIAOMI,
        BrandType.ONEPLUS,
        BrandType.TCL,
        BrandType.GENERIC_SMART_TV
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = device?.name ?: "Universal TV Remote",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${selectedBrandSkin.displayName} • ${device?.ipAddress ?: "Local"} ${if (hasIrEmitter) "(IR Ready)" else "(No IR)"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("remote_topbar_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onTestConnectionClick != null && device != null && device.ipAddress.isNotEmpty()) {
                        IconButton(
                            onClick = onTestConnectionClick,
                            modifier = Modifier.testTag("remote_topbar_test_connection")
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = "Test TV Network Connection")
                        }
                    }
                    IconButton(
                        onClick = onNavigateToTouchpad,
                        modifier = Modifier.testTag("remote_topbar_touchpad")
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = "Touchpad Mode")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ConnectionStatusIndicator(
                selectedDevice = device,
                isScanning = isScanning,
                hasIrEmitter = hasIrEmitter,
                onScanClick = onScanClick,
                onSelectDeviceClick = onNavigateBack
            )

            // BRAND SKIN SELECTOR BAR
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Remote Model Look:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    featuredBrands.forEach { brand ->
                        val isSelected = selectedBrandSkin == brand
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedBrandSkin = brand
                            },
                            label = {
                                Text(
                                    text = when (brand) {
                                        BrandType.SAMSUNG -> "Samsung"
                                        BrandType.LG -> "LG Magic"
                                        BrandType.SONY -> "Sony Bravia"
                                        BrandType.XIAOMI -> "Mi Remote"
                                        BrandType.ONEPLUS -> "OnePlus"
                                        BrandType.TCL -> "TCL / Hisense"
                                        else -> "Universal"
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("brand_chip_${brand.name.lowercase()}")
                        )
                    }
                }
            }

            // REAL PHYSICAL REMOTE CASING ON SCREEN
            RealRemoteChassis(
                brandType = selectedBrandSkin,
                hasIrEmitter = hasIrEmitter,
                onCommand = onCommand,
                modifier = Modifier.testTag("real_remote_chassis")
            )
        }
    }
}
