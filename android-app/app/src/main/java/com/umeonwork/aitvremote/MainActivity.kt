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
import retrofit2.http.POST
import retrofit2.http.Path
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.Types
import android.content.Context
import android.content.SharedPreferences

// Simple models matching the server API
data class Device(val id: String, val name: String?, val socketId: String?, val lastSeen: Long?, val meta: Map<String, Any>?)

data class CommandRequest(val command: String, val params: Map<String, Any>? = null)

data class PairRequestResponse(val id: String, val pin: String)

data class PairStatusResponse(val confirmed: Boolean, val deviceRegistered: Boolean)

data class PairedDevice(val id: String, val name: String?)

interface ApiService {
    @GET("/api/devices")
    suspend fun getDevices(): List<Device>

    @POST("/api/devices/{id}/command")
    suspend fun sendCommand(@Path("id") id: String, @Body payload: CommandRequest)

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
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RemoteScreen(api, applicationContext)
                }
            }
        }
    }

    private fun createApi(): ApiService {
        val base = System.getenv("AITV_API") ?: "http://10.0.2.2:3001"
        val retrofit = Retrofit.Builder()
            .baseUrl(base)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }
}

// SharedPreferences keys and helpers
private const val PREFS = "aitv_prefs"
private const val KEY_PAIRED = "paired_devices"

fun loadPairedDevices(context: Context): List<PairedDevice> {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
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
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
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
                                paired = paired.filter { it.id != p.id }
                                savePairedDevices(appContext, paired)
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
                            // Start polling
                            val start = System.currentTimeMillis()
                            var confirmed = false
                            while (System.currentTimeMillis() - start < 120_000 && !confirmed) {
                                delay(2000)
                                try {
                                    val status = api.pairStatus(resp.id)
                                    if (status.confirmed) {
                                        confirmed = true
                                        // fetch devices and add to paired list
                                        val all = api.getDevices()
                                        val d = all.find { it.id == resp.id }
                                        val pd = PairedDevice(resp.id, d?.name ?: "Unknown")
                                        paired = (paired + pd).distinctBy { it.id }
                                        savePairedDevices(appContext, paired)
                                        Toast.makeText(context, "Paired with ${pd.name}", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    // ignore transient
                                }
                            }
                            if (!confirmed) {
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
                                scope.launch {
                                    try {
                                        api.sendCommand(selectedId!!, CommandRequest(b))
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
