package com.example.cgallery.data

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.cgallery.MainActivity
import com.example.cgallery.R
import kotlinx.coroutines.*

class InboxDetectionService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var detectionEngine: InboxDetectionEngine
    private lateinit var enforcementEngine: EnforcementEngine
    private lateinit var operationQueue: OperationQueue

    override fun onCreate() {
        super.onCreate()
        val inboxManager = InboxManager(this)
        val shizukuManager = ShizukuManager(this)
        val settingsRepo = EnforcementSettingsRepository(this)
        
        detectionEngine = InboxDetectionEngine(this, inboxManager, serviceScope)
        enforcementEngine = EnforcementEngine(this, settingsRepo, shizukuManager, serviceScope)
        operationQueue = OperationQueue(this, serviceScope)
        
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        detectionEngine.start()
        enforcementEngine.start()
        operationQueue.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        detectionEngine.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Inbox Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors for new media to organise"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("cGallery Inbox Engine")
            .setContentText("Monitoring for new media...")
            .setSmallIcon(android.R.drawable.ic_menu_search) // Using system icon for now
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "inbox_detection_channel"

        fun start(context: Context) {
            val intent = Intent(context, InboxDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, InboxDetectionService::class.java)
            context.stopService(intent)
        }
    }
}
