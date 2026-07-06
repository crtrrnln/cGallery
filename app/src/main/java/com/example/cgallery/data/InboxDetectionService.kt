package com.example.cgallery.data
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.cgallery.MainActivity
import kotlinx.coroutines.*

class InboxDetectionService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()); private lateinit var dEng: InboxDetectionEngine
    private lateinit var eEng: EnforcementEngine; private lateinit var oQue: OperationQueue

    override fun onCreate() {
        super.onCreate()
        val m = InboxManager(this); val sm = ShizukuManager(this); val s = AppSettingsRepository(this)
        dEng = InboxDetectionEngine(this, m, scope); eEng = EnforcementEngine(this, s, sm, scope); oQue = OperationQueue(this, scope)
        createChannel(); if (Build.VERSION.SDK_INT >= 29) startForeground(101, createNotif(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(101, createNotif())
        dEng.start(); eEng.start(); oQue.start()
    }

    override fun onStartCommand(i: Intent?, f: Int, sId: Int): Int = START_STICKY
    override fun onDestroy() { super.onDestroy(); dEng.stop(); scope.cancel() }
    override fun onBind(i: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val c = NotificationChannel("inbox_detection_channel", "Inbox Detection", NotificationManager.IMPORTANCE_LOW).apply { description = "Scanning for media" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(c)
        }
    }

    private fun createNotif(): Notification {
        val i = Intent(this, MainActivity::class.java); val pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "inbox_detection_channel").setContentTitle("cGallery").setContentText("Scanning for new files...").setSmallIcon(android.R.drawable.ic_menu_search).setContentIntent(pi).setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

    companion object {
        fun start(ctx: Context) { val i = Intent(ctx, InboxDetectionService::class.java); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i) }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, InboxDetectionService::class.java)) }
    }
}
