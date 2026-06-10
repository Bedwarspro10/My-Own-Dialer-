package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ui.viewmodel.CallStateHolder

class CallService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = intent?.getStringExtra("state")
        val incomingNumber = intent?.getStringExtra("incoming_number") ?: ""
        
        Log.d("CallService", "Processing incoming state: $state for number: $incomingNumber")

        if (state != null) {
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    CallStateHolder.triggerIncomingCall(incomingNumber)
                    val notification = buildCallNotification("Incoming Call from $incomingNumber")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
                    } else {
                        startForeground(1001, notification)
                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    CallStateHolder.triggerOngoingCall(incomingNumber)
                    val notification = buildCallNotification("Call in progress with $incomingNumber")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
                    } else {
                        startForeground(1001, notification)
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    CallStateHolder.triggerIdleCall()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildCallNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "call_channel")
            .setContentTitle("Premium Dialer")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "call_channel",
                "Active Phone Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows notifications for incoming and ongoing calls."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
