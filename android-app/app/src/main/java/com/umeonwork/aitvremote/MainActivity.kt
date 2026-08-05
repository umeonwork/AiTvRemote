package com.umeonwork.aitvremote

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.Types
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray

// Simple models matching the server API
data class Device(val id: String, val name: String?, val socketId: String?, val lastSeen: Long?, val meta: Map<String, Any>?)

data class CommandRequest(val command: String, val params: Map<String, Any>? = null)

data class PairRequestResponse(val id: String, val pin: String)

data class PairStatusResponse(val confirmed: Boolean, val deviceRegistered: Boolean, val token: String?)

data class PairedDevice(val id: String, val name: String?, val token: String)

interface ApiService {
    @GET("/api/devices")
    suspend fun getDevices(): List<Device>

    @POST("/api/devices/{id}/command")
    suspend fun sendCommand(@Path("id") id: String, @Body payload: CommandRequest, @Header("X-Pair-Token") token: String)

    @POST("/api/pair/request")
    suspend fun pairRequest(@Body body: Map<String, String>? = null): PairRequestResponse

    @GET("/api/pair/status/{id}")
    suspend fun pairStatus(@Path("id") id: String): PairStatusResponse
}

class MainActivity : ComponentActivity() {
    private val api by lazy { createApi() }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Migrate any plaintext prefs to encrypted storage before UI loads
        migratePlainToEncrypted(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RemoteScreen(api, applicationContext)
                }
            }
        }

        // Initialize and connect socket manager
        val base = System.getenv("AITV_API") ?: "http://10.0.2.2:3001"
        SocketManager.init(base)
        SocketManager.connect()
    }

    private fun createApi(): ApiService {
        val base = System.getenv("AITV_API") ?: "http://10.0.2.2:3001"
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(base)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return retrofit.create(ApiService::class.java)
    }
}

// SharedPreferences keys and helpers
private const val PREFS = "aitv_prefs"
private const val KEY_PAIRED = "paired_devices"

private fun getEncryptedPrefs(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedSharedPreferences.create(
        context,
        "aitv_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

fun migratePlainToEncrypted(context: Context) {
    try {
        val plain = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (plain.contains(KEY_PAIRED)) {
            val json = plain.getString(KEY_PAIRED, null)
            if (!json.isNullOrEmpty()) {
                val enc = getEncryptedPrefs(context)
                enc.edit().putString(KEY_PAIRED, json).apply()
            }
            plain.edit().remove(KEY_PAIRED).apply()
        }
    } catch (e: Exception) {
        // Migration failed; ignore and continue with empty encrypted store
        e.printStackTrace()
    }
}

fun loadPairedDevices(context: Context): List<PairedDevice> {
    val prefs = getEncryptedPrefs(context)
    val json = prefs.getString(KEY_PAIRED, null) ?: return emptyList()
    return try {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, PairedDevice::class.java)
        val adapter = moshi.adapter<List<PairedDevice>>(type)
        adapter.fromJson(json) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun savePairedDevices(context: Context, list: List<PairedDevice>) {
    val prefs = getEncryptedPrefs(context)
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val type = Types.newParameterizedType(List::class.java, PairedDevice::class.java)
    val adapter = moshi.adapter<List<PairedDevice>>(type)
    val json = adapter.toJson(list)
    prefs.edit().putString(KEY_PAIRED, json).apply()
}

@Composable
fun RemoteScreen(api: ApiService, appContext: Context) {
    var devices by remember { mutableStateOf(listOf<Device>()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var paired by remember { mutableStateOf(loadPairedDevices(appContext)) }
    var pairingInProgress by remember { mutableStateOf<PairRequestResponse?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Register socket callbacks to receive push updates
    DisposableEffect(Unit) {
        val devicesCb: (JSONArray) -> Unit = { arr ->
            // parse JSONArray into List<Device>
            try {
                val list = mutableListOf<Device>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id")
                    val name = if (o.has("name")) o.optString("name") else null
                    val socketId = if (o.has("socketId")) o.optString("socketId") else null
                    list.add(Device(id, name, socketId, null, null))
                }
                devices = list
            } catch (e: Exception) { /* ignore parse errors */ }
        }

        val pairCb: (String, String) -> Unit = { id, token ->
            // If pairing in progress and id matches, finalize pairing
            pairingInProgress?.let {
                if (it.id == id) {
                    scope.launch {
                        // fetch device details
                        try {
                            val all = api.getDevices()
                            val d = all.find { it.id == id }
                            val pd = PairedDevice(id, d?.name ?: "Unknown", token)
                            paired = (paired + pd).distinctBy { it.id }
                            savePairedDevices(appContext, paired)
                            pairingInProgress = null
                            Toast.makeText(context, "Paired with ${pd.name}", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            // still add minimal
                            val pd = PairedDevice(id, "Unknown", token)
                            paired = (paired + pd).distinctBy { it.id }
                            savePairedDevices(appContext, paired)
                            pairingInProgress = null
                            Toast.makeText(context, "Paired", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        SocketManager.onDevicesUpdate(devicesCb)
        SocketManager.onPairConfirmed(pairCb)

        onDispose {
            // clear callbacks
            SocketManager.onDevicesUpdate { }
            SocketManager.onPairConfirmed { _, _ -> }
        }
    }

    // initial load
    LaunchedEffect(Unit) {
        try {
            devices = api.getDevices()
        } catch (e: Exception) {
            // ignore for demo
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(0.18f)) {
            // Paired devices
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFF0F8FF))
                    .padding(12.dp)
            ) {
                Text("Paired Devices", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(paired) { p ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name ?: p.id, fontWeight = FontWeight.Medium)
                            }
                            Text("Remove", color = Color.Red, modifier = Modifier.clickable {
                                val newList = paired.filter { it.id != p.id }
                                paired = newList
                                savePairedDevices(appContext, newList)
                            })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    // start pairing flow
                    scope.launch {
                        try {
                            val resp = api.pairRequest(null)
                            pairingInProgress = resp
                            // Wait for pair:confirmed via SocketManager. Start timeout watcher.
                            val start = System.currentTimeMillis()
                            while (System.currentTimeMillis() - start < 120_000 && pairingInProgress != null) {
                                delay(1000)
                            }
                            if (pairingInProgress != null) {
                                // timed out
                                pairingInProgress = null
                                Toast.makeText(context, "Pairing timed out", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to start pairing: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Pair new device")
                }

                pairingInProgress?.let { p ->
                    Spacer(Modifier.height(8.dp))
                    Text("Enter this PIN on your TV:", fontWeight = FontWeight.Medium)
                    Text(p.pin, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Devices list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                Text("Devices", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(devices) { d ->
                        val isSelected = selectedId == d.id
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                            .clickable { selectedId = d.id }
                            .background(if (isSelected) Color(0xFFE8F0FF) else Color.Transparent)
                            .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(d.name ?: d.id, fontWeight = FontWeight.Medium)
                                Text(if (d.socketId != null) "online" else "offline", fontSize = 12.sp, color = Color.Gray)
                            }
                            Text("${d.id.take(6)}..", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    // refresh device list
                    scope.launch {
                        try {
                            devices = api.getDevices()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to refresh devices", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Refresh") }
            }
        }

        // Remote UI
        Column(modifier = Modifier.weight(0.82f).fillMaxWidth().padding(16.dp)) {
            Text("Remote ${selectedId?.let { "(to ${devices.find { it.id==selectedId }?.name})" } ?: ""}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            val buttons = listOf("power","up","down","left","right","ok","back","home","volume_up","volume_down","mute")

            Column(modifier = Modifier.fillMaxSize()) {
                // Grid-like rows
                for (row in buttons.chunked(4)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (b in row) {
                            Button(onClick = {
                                if (selectedId == null) return@Button
                                val pairedEntry = paired.find { it.id == selectedId }
                                if (pairedEntry == null) {
                                    Toast.makeText(context, "Device not paired. Pair first.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    try {
                                        api.sendCommand(selectedId!!, CommandRequest(b), pairedEntry.token)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to send command", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Text(b.replace("_"," "))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
