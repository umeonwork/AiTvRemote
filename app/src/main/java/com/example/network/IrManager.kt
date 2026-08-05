package com.example.network

import android.content.Context
import android.content.Intent
import android.hardware.ConsumerIrManager
import android.util.Log

class IrManager(private val context: Context) {
    private val irManager: ConsumerIrManager? = 
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        
    val hasIrEmitter: Boolean
        get() = irManager?.hasIrEmitter() == true

    fun transmit(hexCode: String) {
        if (!hasIrEmitter) {
            Log.e("IrManager", "No IR emitter found on this device")
            return
        }
        
        try {
            val intent = Intent(context, IrService::class.java).apply {
                putExtra("hexCode", hexCode)
            }
            context.startService(intent)
            Log.d("IrManager", "Started IrService to transmit code")
        } catch (e: Exception) {
            Log.e("IrManager", "Failed to start IrService: ${e.message}")
        }
    }
}
