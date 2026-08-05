package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DeviceRepository
import com.example.model.DeviceEntity
import com.example.network.DiscoveredDevice
import com.example.network.NsdScanner
import com.example.network.IrManager
import com.example.network.NetworkRemoteManager
import com.example.network.IrCodeMapper
import com.example.ui.remote.BrandType
import com.example.ui.remote.RemoteCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DeviceRepository
    private val nsdScanner: NsdScanner
    private val irManager: IrManager

    private val networkRemoteManager: NetworkRemoteManager

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DeviceRepository(database.deviceDao())
        nsdScanner = NsdScanner(application)
        irManager = IrManager(application)
        networkRemoteManager = NetworkRemoteManager()
    }

    val savedDevices: StateFlow<List<DeviceEntity>> = repository.allDevices
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = nsdScanner.discoveredDevices
    val isScanning: StateFlow<Boolean> = nsdScanner.isScanning

    private val _selectedDevice = MutableStateFlow<DeviceEntity?>(null)
    val selectedDevice = _selectedDevice.asStateFlow()

    private val _commandStatusMessage = MutableStateFlow<String?>(null)
    val commandStatusMessage = _commandStatusMessage.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection = _isTestingConnection.asStateFlow()

    val hasIrEmitter: Boolean
        get() = irManager.hasIrEmitter

    fun clearStatusMessage() {
        _commandStatusMessage.value = null
    }

    fun startScan() {
        nsdScanner.startDiscovery()
    }

    fun stopScan() {
        nsdScanner.stopDiscovery()
    }

    fun addDevice(discovered: DiscoveredDevice) {
        saveDevice(discovered.name, discovered.ipAddress, discovered.type)
    }

    fun saveDevice(name: String, ipAddress: String, type: String) {
        viewModelScope.launch {
            val entity = DeviceEntity(
                name = name,
                ipAddress = ipAddress,
                type = type
            )
            repository.insert(entity)
        }
    }

    fun selectDevice(device: DeviceEntity) {
        viewModelScope.launch {
            _selectedDevice.value = device
            // Update last connected time
            repository.update(device.copy(lastConnected = System.currentTimeMillis()))
            testDeviceConnection(device)
        }
    }

    fun testDeviceConnection(device: DeviceEntity? = _selectedDevice.value) {
        val target = device ?: return
        if (target.ipAddress.isEmpty()) return

        viewModelScope.launch {
            _isTestingConnection.value = true
            _commandStatusMessage.value = "Testing network connection to ${target.name} (${target.ipAddress})..."
            val diag = networkRemoteManager.testConnection(target.ipAddress)
            _commandStatusMessage.value = diag
            _isTestingConnection.value = false
        }
    }

    fun sendCommand(command: RemoteCommand) {
        val device = _selectedDevice.value
        val brandType = BrandType.fromDeviceType(device?.type ?: device?.name)

        var irSent = false
        // 1. Send via IR if emitter is available
        if (irManager.hasIrEmitter) {
            val hexCode = IrCodeMapper.getHexCode(brandType, command)
            if (hexCode != null) {
                irManager.transmit(hexCode)
                irSent = true
            }
        }

        // 2. Send via Network if device has IP
        if (device != null && !device.ipAddress.isNullOrEmpty()) {
            viewModelScope.launch {
                val netResult = networkRemoteManager.sendCommand(device, command)
                if (netResult.isSuccess) {
                    _commandStatusMessage.value = netResult.message
                } else if (!irSent) {
                    _commandStatusMessage.value = netResult.message
                } else {
                    _commandStatusMessage.value = "Transmitted via IR Blaster (${netResult.message})"
                }
            }
        } else if (irSent) {
            _commandStatusMessage.value = "Transmitted $command via IR Blaster"
        } else {
            _commandStatusMessage.value = "No TV connected or invalid IP. Please select a TV."
        }
    }

    fun toggleFavorite(device: DeviceEntity) {
        viewModelScope.launch {
            repository.update(device.copy(isFavorite = !device.isFavorite))
        }
    }
    
    fun deleteDevice(device: DeviceEntity) {
        viewModelScope.launch {
            repository.delete(device.id)
            if (_selectedDevice.value?.id == device.id) {
                _selectedDevice.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        nsdScanner.stopDiscovery()
    }
}
