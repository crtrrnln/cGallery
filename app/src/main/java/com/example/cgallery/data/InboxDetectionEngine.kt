package com.example.cgallery.data
import android.content.Context
import android.database.ContentObserver
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import java.io.File

class InboxDetectionEngine(private val context: Context, private val inboxManager: InboxManager, private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
    private val db = VirtualAlbumDatabase.getDatabase(context); private val folderDao = db.monitoredFolderDao()
    private val settingsRepo = AppSettingsRepository(context); private val shizukuManager = ShizukuManager(context); private var lastForceOpen = 0L
    private val fileObservers = mutableMapOf<String, FileObserver>()
    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) { 
        override fun onChange(self: Boolean) { 
            if (InboxManager.isBulkProcessing) return
            scope.launch { val n = inboxManager.scanNow(); if (n > 0) forceOpenIfNeeded() } 
        } 
    }

    fun start() {
        context.contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, contentObserver)
        context.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, contentObserver)
        scope.launch { folderDao.getAllFolders().collectLatest { folders -> updateFileObservers(folders.filter { it.isEnabled }) } }
        scope.launch { inboxManager.scanNow(fullScan = true) }
    }

    private fun updateFileObservers(folders: List<MonitoredFolderEntity>) {
        val paths = folders.map { it.folderPath }.toSet()
        fileObservers.keys.filter { it !in paths }.forEach { p -> fileObservers[p]?.stopWatching(); fileObservers.remove(p) }
        folders.forEach { f ->
            if (!fileObservers.containsKey(f.folderPath)) {
                val obs = if (android.os.Build.VERSION.SDK_INT >= 29) {
                    object : FileObserver(File(f.folderPath), CREATE or MOVED_TO or CLOSE_WRITE) { 
                        override fun onEvent(e: Int, p: String?) { 
                            if (p != null) {
                                val fullPath = File(f.folderPath, p).absolutePath
                                android.util.Log.d("InboxDetection", "Event 0x${Integer.toHexString(e)}: $fullPath")
                                // For downloads, CLOSE_WRITE is the "ready" signal. CREATE is just the beginning.
                                android.media.MediaScannerConnection.scanFile(context, arrayOf(fullPath), null) { _, _ ->
                                    scope.launch { 
                                        delay(800) // Increased delay to ensure MediaStore stability
                                        val n = inboxManager.scanNow()
                                        android.util.Log.d("InboxDetection", "Scan result: $n new items")
                                        if (n > 0) forceOpenIfNeeded() 
                                    }
                                }
                            }
                        } 
                    }
                } else {
                    @Suppress("DEPRECATION")
                    object : FileObserver(f.folderPath, CREATE or MOVED_TO or CLOSE_WRITE) { 
                        override fun onEvent(e: Int, p: String?) { 
                            if (p != null) {
                                val fullPath = File(f.folderPath, p).absolutePath
                                android.util.Log.d("InboxDetection", "Event 0x${Integer.toHexString(e)}: $fullPath")
                                android.media.MediaScannerConnection.scanFile(context, arrayOf(fullPath), null) { _, _ ->
                                    scope.launch { 
                                        delay(800)
                                        val n = inboxManager.scanNow()
                                        android.util.Log.d("InboxDetection", "Scan result: $n new items")
                                        if (n > 0) forceOpenIfNeeded() 
                                    }
                                }
                            }
                        } 
                    }
                }
                obs.startWatching(); fileObservers[f.folderPath] = obs
            }
        }
    }

    private suspend fun forceOpenIfNeeded() {
        if (System.currentTimeMillis() - lastForceOpen < 3500) return
        val s = settingsRepo.settingsFlow.first()
        val pendingCount = inboxManager.getPendingCount()
        val shizukuOk = shizukuManager.hasPermission()
        
        android.util.Log.d("InboxDetection", "Launch Check: pending=$pendingCount, threshold=${s.snoozeItemThreshold}, shizuku=$shizukuOk, enforcement=${s.isEnforcementEnabled}")

        val snoozedTime = s.snoozeExpirationTime > System.currentTimeMillis()
        val snoozedItems = s.snoozeItemThreshold > 0 && pendingCount < s.snoozeItemThreshold
        
        if (!s.isEnforcementEnabled || !s.isShizukuEnabled || !s.launchAutomatically || snoozedTime || snoozedItems) return
        
        if (shizukuOk) { 
            android.util.Log.d("InboxDetection", "TRIGGERING FORCED LAUNCH")
            shizukuManager.launchAppToInbox()
            lastForceOpen = System.currentTimeMillis() 
        }
    }
    fun stop() {
        context.contentResolver.unregisterContentObserver(contentObserver)
        fileObservers.values.forEach { it.stopWatching() }; fileObservers.clear()
    }
}


