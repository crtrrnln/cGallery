package com.example.cgallery.data
import android.content.Context
import android.database.ContentObserver
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File

class InboxDetectionEngine(private val context: Context, private val inboxManager: InboxManager, private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
    private val db = VirtualAlbumDatabase.getDatabase(context); private val folderDao = db.monitoredFolderDao()
    private val fileObservers = mutableMapOf<String, FileObserver>()
    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) { 
        override fun onChange(self: Boolean) { 
            if (InboxManager.isBulkProcessing) return
            scope.launch { inboxManager.scanNow() } 
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
                    object : FileObserver(File(f.folderPath), CREATE or MOVED_TO) { override fun onEvent(e: Int, p: String?) { scope.launch { delay(1500); inboxManager.scanNow() } } }
                } else {
                    @Suppress("DEPRECATION")
                    object : FileObserver(f.folderPath, CREATE or MOVED_TO) { override fun onEvent(e: Int, p: String?) { scope.launch { delay(1500); inboxManager.scanNow() } } }
                }
                obs.startWatching(); fileObservers[f.folderPath] = obs
            }
        }
    }

    fun stop() {
        context.contentResolver.unregisterContentObserver(contentObserver)
        fileObservers.values.forEach { it.stopWatching() }; fileObservers.clear()
    }
}
