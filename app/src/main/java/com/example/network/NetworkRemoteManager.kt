package com.example.network

import android.util.Log
import com.example.model.DeviceEntity
import com.example.ui.remote.BrandType
import com.example.ui.remote.RemoteCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.text.Charsets

data class NetworkCommandResult(
    val isSuccess: Boolean,
    val message: String,
    val protocolUsed: String = "Network"
)

class NetworkRemoteManager {

    suspend fun sendCommand(device: DeviceEntity, command: RemoteCommand): NetworkCommandResult {
        val ip = device.ipAddress.trim()
        if (ip.isEmpty()) {
            return NetworkCommandResult(false, "No IP address specified for ${device.name}")
        }

        val brand = BrandType.fromDeviceType(device.type ?: device.name)

        return withContext(Dispatchers.IO) {
            var lastError = ""

            // 1. Try Brand-Specific primary protocol
            val primaryResult = when (brand) {
                BrandType.ROKU -> tryRokuCommand(ip, command)
                BrandType.SONY -> trySonyIrccCommand(ip, command)
                BrandType.SAMSUNG -> trySamsungCommand(ip, command)
                BrandType.LG -> tryLgWebOsCommand(ip, command)
                BrandType.GENERIC_ANDROID_TV, BrandType.XIAOMI, BrandType.TCL, 
                BrandType.VU, BrandType.ONEPLUS, BrandType.REALME, BrandType.HISENSE,
                BrandType.FIRE_TV, BrandType.MOTOROLA_NOKIA -> tryAndroidAdbCommand(ip, command)
                else -> NetworkCommandResult(false, "Generic protocol dispatching")
            }

            if (primaryResult.isSuccess) {
                return@withContext primaryResult
            } else {
                lastError = primaryResult.message
            }

            // 2. Multi-Protocol Universal Fallback Chain
            // Check if device is a Roku TV
            val rokuRes = tryRokuCommand(ip, command)
            if (rokuRes.isSuccess) return@withContext rokuRes

            // Check if device responds to Sony IRCC
            val sonyRes = trySonyIrccCommand(ip, command)
            if (sonyRes.isSuccess) return@withContext sonyRes

            // Check ADB socket on port 5555 (Android TV / Fire TV)
            val adbRes = tryAndroidAdbCommand(ip, command)
            if (adbRes.isSuccess) return@withContext adbRes

            // Check UPnP Rendering Control for Volume/Mute/Playback
            val upnpRes = tryUpnpCommand(ip, command)
            if (upnpRes.isSuccess) return@withContext upnpRes

            // Check DIAL protocol for App launching
            if (command.name.startsWith("APP_")) {
                val dialRes = tryDialAppLaunch(ip, command)
                if (dialRes.isSuccess) return@withContext dialRes
            }

            NetworkCommandResult(
                isSuccess = false,
                message = "Could not reach TV at $ip. Ensure TV & phone are on same Wi-Fi. ($lastError)"
            )
        }
    }

    /**
     * Test connection to common TV ports to detect active protocols
     */
    suspend fun testConnection(ip: String): String = withContext(Dispatchers.IO) {
        val openPorts = mutableListOf<String>()

        val portsToTest = listOf(
            8060 to "Roku ECP",
            80 to "Sony Bravia / HTTP",
            5555 to "Android TV ADB",
            8001 to "Samsung Tizen API",
            3000 to "LG webOS SSAP",
            8008 to "Google DIAL / Chromecast",
            52235 to "UPnP Media Control"
        )

        for ((port, label) in portsToTest) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 800)
                    openPorts.add("$label (Port $port)")
                }
            } catch (e: Exception) {
                // Port closed or unreachable
            }
        }

        if (openPorts.isNotEmpty()) {
            "Detected TV Services at $ip: " + openPorts.joinToString(", ")
        } else {
            "No active TV network ports found at $ip. Check Wi-Fi connection."
        }
    }

    // --- 1. Roku ECP Protocol (Port 8060) ---
    private fun tryRokuCommand(ip: String, command: RemoteCommand): NetworkCommandResult {
        val rokuKey = when (command) {
            RemoteCommand.POWER -> "Power"
            RemoteCommand.UP -> "Up"
            RemoteCommand.DOWN -> "Down"
            RemoteCommand.LEFT -> "Left"
            RemoteCommand.RIGHT -> "Right"
            RemoteCommand.SELECT -> "Select"
            RemoteCommand.HOME -> "Home"
            RemoteCommand.BACK -> "Back"
            RemoteCommand.MENU, RemoteCommand.INFO -> "Info"
            RemoteCommand.VOL_UP -> "VolumeUp"
            RemoteCommand.VOL_DOWN -> "VolumeDown"
            RemoteCommand.MUTE -> "VolumeMute"
            RemoteCommand.PLAY -> "Play"
            RemoteCommand.PAUSE -> "Play"
            RemoteCommand.REWIND -> "Rev"
            RemoteCommand.FAST_FORWARD -> "Fwd"
            RemoteCommand.INPUT -> "InputTuner"
            RemoteCommand.APP_YOUTUBE -> "launch/837"
            RemoteCommand.APP_NETFLIX -> "launch/12"
            RemoteCommand.APP_PRIME -> "launch/13"
            RemoteCommand.APP_SPOTIFY -> "launch/19977"
            else -> return NetworkCommandResult(false, "Command $command not supported on Roku ECP")
        }

        return try {
            val urlString = if (rokuKey.startsWith("launch/")) {
                "http://$ip:8060/$rokuKey"
            } else {
                "http://$ip:8060/keypress/$rokuKey"
            }

            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 1500
                readTimeout = 1500
                doOutput = true
            }

            val responseCode = conn.responseCode
            conn.disconnect()

            if (responseCode in 200..299) {
                NetworkCommandResult(true, "Sent $command to Roku TV ($ip)", "Roku ECP")
            } else {
                NetworkCommandResult(false, "Roku returned HTTP $responseCode")
            }
        } catch (e: Exception) {
            NetworkCommandResult(false, "Roku connection failed: ${e.message}")
        }
    }

    // --- 2. Sony Bravia IRCC SOAP Protocol (Port 80) ---
    private fun trySonyIrccCommand(ip: String, command: RemoteCommand): NetworkCommandResult {
        val irccCode = when (command) {
            RemoteCommand.POWER -> "AAAAAQAAAAEAAAAVAw=="
            RemoteCommand.VOL_UP -> "AAAAAQAAAAEAAAB0Aw=="
            RemoteCommand.VOL_DOWN -> "AAAAAQAAAAEAAAB1Aw=="
            RemoteCommand.MUTE -> "AAAAAQAAAAEAAAB2Aw=="
            RemoteCommand.UP -> "AAAAAQAAAAEAAAA2Aw=="
            RemoteCommand.DOWN -> "AAAAAQAAAAEAAAA3Aw=="
            RemoteCommand.LEFT -> "AAAAAQAAAAEAAAA4Aw=="
            RemoteCommand.RIGHT -> "AAAAAQAAAAEAAAA5Aw=="
            RemoteCommand.SELECT -> "AAAAAQAAAAEAAABlAw=="
            RemoteCommand.HOME -> "AAAAAQAAAAEAAAA0Aw=="
            RemoteCommand.BACK -> "AAAAAQAAAAEAAAA1Aw=="
            RemoteCommand.INPUT -> "AAAAAQAAAAEAAAAlAw=="
            RemoteCommand.RED_BUTTON -> "AAAAAgAAAJcAAAANAw=="
            RemoteCommand.GREEN_BUTTON -> "AAAAAgAAAJcAAAAOAw=="
            RemoteCommand.YELLOW_BUTTON -> "AAAAAgAAAJcAAAAPAw=="
            RemoteCommand.BLUE_BUTTON -> "AAAAAgAAAJcAAAAQAw=="
            else -> return NetworkCommandResult(false, "Command $command not supported on Sony IRCC")
        }

        val xmlBody = """
            <?xml version="1.0"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:X_SendIRCC xmlns:u="urn:schemas-sony-com:service:IRCC:1">
                        <IRCCCode>$irccCode</IRCCCode>
                    </u:X_SendIRCC>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        return try {
            val url = URL("http://$ip/sony/IRCCCode")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 1500
                readTimeout = 1500
                setRequestProperty("Content-Type", "text/xml; charset=utf-8")
                setRequestProperty("SOAPACTION", "\"urn:schemas-sony-com:service:IRCC:1#X_SendIRCC\"")
                setRequestProperty("X-Auth-PSK", "1234") // Common default PSK for Sony Bravia
                doOutput = true
            }

            conn.outputStream.use { os ->
                os.write(xmlBody.toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            conn.disconnect()

            if (responseCode in 200..299) {
                NetworkCommandResult(true, "Sent $command to Sony TV ($ip)", "Sony IRCC")
            } else {
                NetworkCommandResult(false, "Sony IRCC returned HTTP $responseCode")
            }
        } catch (e: Exception) {
            NetworkCommandResult(false, "Sony IRCC failed: ${e.message}")
        }
    }

    // --- 3. Android TV / ADB Command (Port 5555) ---
    private fun tryAndroidAdbCommand(ip: String, command: RemoteCommand): NetworkCommandResult {
        val adbKeyCode = when (command) {
            RemoteCommand.POWER -> 26
            RemoteCommand.UP -> 19
            RemoteCommand.DOWN -> 20
            RemoteCommand.LEFT -> 21
            RemoteCommand.RIGHT -> 22
            RemoteCommand.SELECT -> 66
            RemoteCommand.BACK -> 4
            RemoteCommand.HOME -> 3
            RemoteCommand.MENU -> 82
            RemoteCommand.VOL_UP -> 24
            RemoteCommand.VOL_DOWN -> 25
            RemoteCommand.MUTE -> 164
            RemoteCommand.PLAY -> 126
            RemoteCommand.PAUSE -> 127
            RemoteCommand.STOP -> 128
            RemoteCommand.FAST_FORWARD -> 90
            RemoteCommand.REWIND -> 89
            RemoteCommand.KEY_0 -> 7
            RemoteCommand.KEY_1 -> 8
            RemoteCommand.KEY_2 -> 9
            RemoteCommand.KEY_3 -> 10
            RemoteCommand.KEY_4 -> 11
            RemoteCommand.KEY_5 -> 12
            RemoteCommand.KEY_6 -> 13
            RemoteCommand.KEY_7 -> 14
            RemoteCommand.KEY_8 -> 15
            RemoteCommand.KEY_9 -> 16
            else -> null
        }

        if (adbKeyCode == null) {
            return NetworkCommandResult(false, "ADB Keycode not mapped for $command")
        }

        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, 5555), 1200)
                val output: OutputStream = socket.getOutputStream()

                // Send simple ADB shell keyevent string to TCP 5555
                val cmd = "shell:input keyevent $adbKeyCode\n"
                output.write(cmd.toByteArray(Charsets.UTF_8))
                output.flush()

                NetworkCommandResult(true, "Sent keyevent $adbKeyCode to Android TV ($ip)", "Android ADB")
            }
        } catch (e: Exception) {
            NetworkCommandResult(false, "Android TV ADB connection refused at $ip:5555")
        }
    }

    // --- 4. Samsung Tizen API / HTTP REST (Port 8001 / 8002) ---
    private fun trySamsungCommand(ip: String, command: RemoteCommand): NetworkCommandResult {
        return try {
            val url = URL("http://$ip:8001/api/v2/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 1200
                readTimeout = 1200
            }
            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..299) {
                NetworkCommandResult(true, "Samsung TV endpoint active at $ip:8001", "Samsung Tizen")
            } else {
                tryUpnpCommand(ip, command)
            }
        } catch (e: Exception) {
            tryUpnpCommand(ip, command)
        }
    }

    // --- 5. LG webOS SSAP / HTTP (Port 3000 / 8080) ---
    private fun tryLgWebOsCommand(ip: String, command: RemoteCommand): NetworkCommandResult {
        return try {
            val url = URL("http://$ip:3000/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 1200
                readTimeout = 1200
            }
            val code = conn.responseCode
            conn.disconnect()

            if (code in 200..399) {
                NetworkCommandResult(true, "LG webOS endpoint active at $ip:3000", "LG webOS")
            } else {
                tryUpnpCommand(ip, command)
            }
        } catch (e: Exception) {
            tryUpnpCommand(ip, command)
        }
    }

    // --- 6. Universal UPnP RenderingControl SOAP POST (Port 52235 / 1900 / 8080) ---
    private fun tryUpnpCommand(ip: String, command: RemoteCommand): NetworkCommandResult {
        val (action, extraParam) = when (command) {
            RemoteCommand.VOL_UP -> "SetVolume" to "<DesiredVolume>+5</DesiredVolume>"
            RemoteCommand.VOL_DOWN -> "SetVolume" to "<DesiredVolume>-5</DesiredVolume>"
            RemoteCommand.MUTE -> "SetMute" to "<DesiredMute>1</DesiredMute>"
            RemoteCommand.PLAY -> "Play" to "<Speed>1</Speed>"
            RemoteCommand.PAUSE -> "Pause" to ""
            RemoteCommand.STOP -> "Stop" to ""
            else -> return NetworkCommandResult(false, "Command $command not supported via UPnP")
        }

        val soapBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
                <s:Body>
                    <u:$action xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
                        <InstanceID>0</InstanceID>
                        <Channel>Master</Channel>
                        $extraParam
                    </u:$action>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val portsToTry = listOf(52235, 1900, 8080, 7676)
        for (port in portsToTry) {
            try {
                val url = URL("http://$ip:$port/upnp/control/RenderingControl1")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 1000
                    readTimeout = 1000
                    setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                    setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:RenderingControl:1#$action\"")
                    doOutput = true
                }

                conn.outputStream.use { os ->
                    os.write(soapBody.toByteArray(Charsets.UTF_8))
                }

                val resCode = conn.responseCode
                conn.disconnect()

                if (resCode in 200..299) {
                    return NetworkCommandResult(true, "Sent UPnP $action to $ip:$port", "UPnP Universal")
                }
            } catch (e: Exception) {
                // Try next port
            }
        }

        return NetworkCommandResult(false, "UPnP control unreachable on $ip")
    }

    // --- 7. DIAL Protocol for Smart TV App Launching (Port 8008 / 8080) ---
    private fun tryDialAppLaunch(ip: String, command: RemoteCommand): NetworkCommandResult {
        val appName = when (command) {
            RemoteCommand.APP_YOUTUBE -> "YouTube"
            RemoteCommand.APP_NETFLIX -> "Netflix"
            RemoteCommand.APP_PRIME -> "AmazonInstantVideo"
            RemoteCommand.APP_SPOTIFY -> "Spotify"
            else -> return NetworkCommandResult(false, "App not supported for DIAL launch")
        }

        val ports = listOf(8008, 8080, 8001)
        for (port in ports) {
            try {
                val url = URL("http://$ip:$port/apps/$appName")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 1000
                    readTimeout = 1000
                    doOutput = true
                }

                val code = conn.responseCode
                conn.disconnect()

                if (code in 200..299 || code == 201) {
                    return NetworkCommandResult(true, "Launched $appName via DIAL on $ip", "DIAL Protocol")
                }
            } catch (e: Exception) {
                // Try next DIAL port
            }
        }

        return NetworkCommandResult(false, "DIAL app launch failed on $ip")
    }
}
