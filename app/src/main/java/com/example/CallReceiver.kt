package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            @Suppress("DEPRECATION")
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
            Log.d("CallReceiver", "Phone State Changed: $state, incoming: $incomingNumber")
            
            if (state != null) {
                val serviceIntent = Intent(context, CallService::class.java).apply {
                    putExtra("state", state)
                    putExtra("incoming_number", incomingNumber)
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    Log.e("CallReceiver", "Failed to start CallService in foreground", e)
                }
            }
        }
    }
}
