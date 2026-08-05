package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DiscoveredDevice(val name: String, val ipAddress: String, val type: String)

class NsdScanner(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val activeListeners = mutableMapOf<String, NsdManager.DiscoveryListener>()

    // Common TV discovery services for global & Indian Smart TVs
    private val serviceTypes = listOf(
        "_googlecast._tcp.",
        "_androidtvremote2._tcp.",
        "_airplay._tcp.",
        "_samsungmsf._tcp.",
        "_webos-second-screen._tcp.",
        "_hisense-remote._tcp.",
        "_dial-multiscreen-org._tcp.",
        "_spotify-connect._tcp."
    )

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true
        _discoveredDevices.value = emptyList()

        // --- MOCK DEVICES FOR EMULATOR TESTING ---
        // Since the cloud emulator cannot reach the local network, inject simulated TVs
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (_isScanning.value) {
                val mockDevices = listOf(
                    DiscoveredDevice("Living Room TCL (Demo)", "192.168.1.100", "Android TV"),
                    DiscoveredDevice("Samsung QLED (Demo)", "192.168.1.102", "Samsung Smart TV (Tizen)"),
                    DiscoveredDevice("Bedroom LG (Demo)", "192.168.1.105", "LG webOS TV")
                )
                _discoveredDevices.update { current ->
                    (current + mockDevices).distinctBy { it.ipAddress }
                }
            }
        }, 2000)

        serviceTypes.forEach { type ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {
                    Log.d("NsdScanner", "Discovery started for $regType")
                }

                override fun onServiceFound(service: NsdServiceInfo) {
                    Log.d("NsdScanner", "Service found: ${service.serviceName}")
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e("NsdScanner", "Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            Log.d("NsdScanner", "Resolved: ${serviceInfo.serviceName} at ${serviceInfo.host?.hostAddress}")
                            val ip = serviceInfo.host?.hostAddress
                            if (ip != null) {
                                val newDevice = DiscoveredDevice(
                                    name = serviceInfo.serviceName ?: "Unknown TV",
                                    ipAddress = ip,
                                    type = deriveType(serviceInfo.serviceType)
                                )
                                _discoveredDevices.update { current ->
                                    if (current.none { it.ipAddress == ip }) {
                                        current + newDevice
                                    } else {
                                        current
                                    }
                                }
                            }
                        }
                    })
                }

                override fun onServiceLost(service: NsdServiceInfo) {
                    Log.e("NsdScanner", "Service lost: ${service.serviceName}")
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    Log.i("NsdScanner", "Discovery stopped: $serviceType")
                }

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e("NsdScanner", "Start discovery failed: $errorCode")
                    nsdManager.stopServiceDiscovery(this)
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    Log.e("NsdScanner", "Stop discovery failed: $errorCode")
                    nsdManager.stopServiceDiscovery(this)
                }
            }
            activeListeners[type] = listener
            nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    fun stopDiscovery() {
        if (!_isScanning.value) return
        activeListeners.values.forEach { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.e("NsdScanner", "Error stopping discovery", e)
            }
        }
        activeListeners.clear()
        _isScanning.value = false
    }

    private fun deriveType(regType: String): String {
        val lower = regType.lowercase()
        return when {
            lower.contains("samsung") -> "Samsung Smart TV (Tizen)"
            lower.contains("webos") || lower.contains("lg") -> "LG webOS TV"
            lower.contains("hisense") -> "Hisense (VIDAA / Android)"
            lower.contains("googlecast") -> "Google TV / Chromecast"
            lower.contains("androidtv") -> "Android TV"
            lower.contains("airplay") -> "AirPlay TV"
            lower.contains("dial") -> "Universal DIAL TV"
            else -> "Smart TV"
        }
    }
}
