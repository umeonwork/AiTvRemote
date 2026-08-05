package com.example.ui.screens

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.ui.remote.BaseRemoteLayout
import com.example.ui.remote.RemoteCommand
import com.example.viewmodel.MainViewModel

@Composable
fun RemoteScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTouchpad: () -> Unit
) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val commandStatusMessage by viewModel.commandStatusMessage.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()
    val context = LocalContext.current

    // Display Toast when command status message arrives
    LaunchedEffect(commandStatusMessage) {
        commandStatusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    BaseRemoteLayout(
        device = selectedDevice,
        hasIrEmitter = viewModel.hasIrEmitter,
        isScanning = isScanning,
        onScanClick = { viewModel.startScan() },
        onTestConnectionClick = {
            viewModel.testDeviceConnection()
        },
        onCommand = { command ->
            viewModel.sendCommand(command)
        },
        onNavigateBack = onNavigateBack,
        onNavigateToTouchpad = onNavigateToTouchpad
    )
}
