package com.example.network

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.ConsumerIrManager
import android.os.IBinder
import android.util.Log

class IrService : Service() {

    private var irManager: ConsumerIrManager? = null

    override fun onCreate() {
        super.onCreate()
        irManager = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hexCode = intent?.getStringExtra("hexCode")
        
        if (hexCode != null) {
            transmit(hexCode)
        }
        
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun transmit(hexCode: String) {
        val hasEmitter = irManager?.hasIrEmitter() == true
        if (!hasEmitter) {
            Log.e("IrService", "No IR emitter found on this device")
            return
        }
        
        try {
            val (frequency, pattern) = hex2dec(hexCode)
            if (pattern.isNotEmpty()) {
                irManager?.transmit(frequency, pattern)
                Log.d("IrService", "Transmitted IR code via Service")
            }
        } catch (e: Exception) {
            Log.e("IrService", "Failed to transmit IR code: ${e.message}")
        }
    }

    private fun hex2dec(irData: String): Pair<Int, IntArray> {
        val list = irData.trim().split("\\s+".toRegex())
        
        if (list.size < 4) return Pair(38000, intArrayOf())
        
        val frequency = try {
            val freqHex = list[1]
            (1000000 / (freqHex.toInt(16) * 0.241246)).toInt()
        } catch (e: Exception) {
            38000
        }
        
        val pulses = list.size - 4
        val pattern = IntArray(pulses)
        val period = 1000000.0 / frequency
        for (i in 0 until pulses) {
            try {
                pattern[i] = (list[i + 4].toInt(16) * period).toInt()
            } catch (e: Exception) {
                pattern[i] = 0
            }
        }
        return Pair(frequency, pattern)
    }
}
