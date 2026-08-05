package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DeviceEntity
import com.example.network.DiscoveredDevice
import com.example.ui.components.ConnectionStatusIndicator
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToRemote: () -> Unit
) {
    val savedDevices by viewModel.savedDevices.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()

    var showScanDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var deviceToPair by remember { mutableStateOf<DiscoveredDevice?>(null) }
    var pairingPin by remember { mutableStateOf("") }

    // Manual Add state
    var manualName by remember { mutableStateOf("") }
    var manualIp by remember { mutableStateOf("") }
    var manualPinCode by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf(com.example.ui.remote.BrandType.XIAOMI) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TVMaster AI") },
                actions = {
                    IconButton(
                        onClick = { showManualAddDialog = true },
                        modifier = Modifier.testTag("home_add_manual_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Manual IP / Brand Add")
                    }
                    IconButton(
                        onClick = { 
                            showScanDialog = true
                            viewModel.startScan()
                        },
                        modifier = Modifier.testTag("home_scan_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan Network")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ConnectionStatusIndicator(
                selectedDevice = selectedDevice,
                isScanning = isScanning,
                hasIrEmitter = viewModel.hasIrEmitter,
                onScanClick = {
                    showScanDialog = true
                    viewModel.startScan()
                },
                onSelectDeviceClick = {
                    showScanDialog = true
                    viewModel.startScan()
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (savedDevices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No TVs Paired",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Supports TCL, iFFALCON, Mi/Xiaomi, Vu, OnePlus, Realme, Hisense, Samsung, LG, Sony & all Smart TVs in India",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { 
                                    showScanDialog = true
                                    viewModel.startScan()
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Auto Scan", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { showManualAddDialog = true },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manual IP", maxLines = 1)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "My Paired TVs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { showManualAddDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add TV", maxLines = 1)
                            }
                        }
                    }
                    items(savedDevices) { device ->
                        DeviceCard(
                            device = device,
                            onClick = {
                                viewModel.selectDevice(device)
                                onNavigateToRemote()
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(device) },
                            onDelete = { viewModel.deleteDevice(device) }
                        )
                    }
                }
            }
        }
    }

    // Manual Add Dialog
    if (showManualAddDialog) {
        var expandedBrandDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("Add TV by IP & Brand") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = { manualIp = it },
                        label = { Text("TV IP Address (e.g. 192.168.1.100)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("TV Name (e.g. Living Room iFFALCON)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualPinCode,
                        onValueChange = { manualPinCode = it },
                        label = { Text("Pairing PIN Code (Optional)") },
                        placeholder = { Text("e.g. 1234 (if TV displays code)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedBrandDropdown,
                        onExpandedChange = { expandedBrandDropdown = !expandedBrandDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedBrand.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select TV Brand") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrandDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedBrandDropdown,
                            onDismissRequest = { expandedBrandDropdown = false }
                        ) {
                            com.example.ui.remote.BrandType.entries.forEach { brand ->
                                DropdownMenuItem(
                                    text = { Text(brand.displayName) },
                                    onClick = {
                                        selectedBrand = brand
                                        expandedBrandDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualIp.isNotBlank()) {
                            val nameToUse = manualName.ifBlank { selectedBrand.displayName }
                            viewModel.addDevice(
                                DiscoveredDevice(
                                    name = nameToUse,
                                    ipAddress = manualIp.trim(),
                                    type = selectedBrand.displayName
                                )
                            )
                            manualIp = ""
                            manualName = ""
                            manualPinCode = ""
                            showManualAddDialog = false
                        }
                    }
                ) {
                    Text("Add & Pair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showScanDialog) {
        AlertDialog(
            onDismissRequest = { 
                showScanDialog = false
                viewModel.stopScan()
            },
            title = { Text("Discovering TVs...") },
            text = {
                Column {
                    if (isScanning) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (discoveredDevices.isEmpty()) {
                        Text("Looking for devices on your Wi-Fi network...")
                    } else {
                        LazyColumn {
                            items(discoveredDevices) { discovered ->
                                DiscoveredDeviceItem(
                                    device = discovered,
                                    onAdd = {
                                        val requiresPairing = discovered.type.contains("Android") || 
                                                              discovered.type.contains("webOS") || 
                                                              discovered.type.contains("Samsung")
                                        if (requiresPairing) {
                                            deviceToPair = discovered
                                        } else {
                                            viewModel.addDevice(discovered)
                                            showScanDialog = false
                                            viewModel.stopScan()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showScanDialog = false
                    viewModel.stopScan()
                }) {
                    Text("Close")
                }
            }
        )
    }

    if (deviceToPair != null) {
        val device = deviceToPair!!
        AlertDialog(
            onDismissRequest = { deviceToPair = null },
            title = { Text("Pair with ${device.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Please enter the PIN code displayed on your TV screen to pair.")
                    OutlinedTextField(
                        value = pairingPin,
                        onValueChange = { pairingPin = it },
                        label = { Text("PIN Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addDevice(device)
                        deviceToPair = null
                        showScanDialog = false
                        viewModel.stopScan()
                        pairingPin = ""
                    }
                ) {
                    Text("Pair & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToPair = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DeviceCard(
    device: DeviceEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${device.type} • ${device.ipAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (device.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (device.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun DiscoveredDeviceItem(
    device: DiscoveredDevice,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = device.name, fontWeight = FontWeight.SemiBold)
            Text(text = device.ipAddress, style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = onAdd) {
            Text("Add")
        }
    }
}
