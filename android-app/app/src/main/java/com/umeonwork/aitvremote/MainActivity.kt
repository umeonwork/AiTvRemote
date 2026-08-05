package com.umeonwork.aitvremote

import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Simple models matching the server API
data class Device(val id: String, val name: String, val socketId: String?, val lastSeen: Long?, val meta: Map<String, Any>?)

data class CommandRequest(val command: String, val params: Map<String, Any>? = null)

interface ApiService {
    @GET("/api/devices")
    suspend fun getDevices(): List<Device>

    @POST("/api/devices/{id}/command")
    suspend fun sendCommand(@Path("id") id: String, @Body payload: CommandRequest)
}

class MainActivity : ComponentActivity() {
    private val api by lazy { createApi() }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RemoteScreen(api)
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

@Composable
fun RemoteScreen(api: ApiService) {
    var devices by remember { mutableStateOf(listOf<Device>()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            devices = api.getDevices()
        } catch (e: Exception) {
            // ignore for demo
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Devices list
        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(Color(0xFFF5F5F5))
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
                    }
                }
            }
        }

        // Remote UI
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                                        // show simple log; in production show Snackbar/toast
                                        e.printStackTrace()
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
