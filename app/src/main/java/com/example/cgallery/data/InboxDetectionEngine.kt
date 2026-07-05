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

class InboxDetectionEngine(
    private val context: Context,
    private val inboxManager: InboxManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val folderDao = db.monitoredFolderDao()
    private val fileObservers = mutableMapOf<String, FileObserver>()

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            scope.launch {
                inboxManager.scanNow()
            }
        }
    }

    fun start() {
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )

        scope.launch {
            folderDao.getAllFolders().collectLatest { folders ->
                updateFileObservers(folders.filter { it.isEnabled })
            }
        }
        
        scope.launch {
            inboxManager.scanNow(fullScan = true)
        }
    }

    private fun updateFileObservers(folders: List<MonitoredFolderEntity>) {
        val currentPaths = folders.map { it.folderPath }.toSet()
        
        val toRemove = fileObservers.keys.filter { it !in currentPaths }
        toRemove.forEach { path ->
            fileObservers[path]?.stopWatching()
            fileObservers.remove(path)
        }

        folders.forEach { folder ->
            if (!fileObservers.containsKey(folder.folderPath)) {
                val observer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    object : FileObserver(File(folder.folderPath), CREATE or MOVED_TO) {
                        override fun onEvent(event: Int, path: String?) {
                            scope.launch {
                                // mediastore takes a sec to update so we wait
                                delay(1500)
                                inboxManager.scanNow()
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    object : FileObserver(folder.folderPath, CREATE or MOVED_TO) {
                        override fun onEvent(event: Int, path: String?) {
                            scope.launch {
                                delay(1500)
                                inboxManager.scanNow()
                            }
                        }
                    }
                }
                observer.startWatching()
                fileObservers[folder.folderPath] = observer
            }
        }
    }

    fun stop() {
        context.contentResolver.unregisterContentObserver(contentObserver)
        fileObservers.values.forEach { it.stopWatching() }
        fileObservers.clear()
    }
}
