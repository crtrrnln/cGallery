package com.example.cgallery.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EnforcementEngine(
    private val context: Context,
    private val settingsRepository: EnforcementSettingsRepository,
    private val shizukuManager: ShizukuManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val inboxDao = db.inboxDao()

    fun start() {
        scope.launch {
            // Monitor for new pending items
            inboxDao.getPendingItems().collectLatest { items ->
                if (items.isNotEmpty()) {
                    checkAndTriggerSession(items)
                }
            }
        }
    }

    private suspend fun checkAndTriggerSession(items: List<InboxItemEntity>) {
        val settings = settingsRepository.settingsFlow.first()
        
        if (!settings.isEnforcementEnabled) return
        
        // Check snooze
        if (settings.snoozeExpirationTime > System.currentTimeMillis()) {
            return
        }
        
        if (settings.snoozeItemThreshold > 0 && settings.currentSnoozeCount < settings.snoozeItemThreshold) {
            return
        }

        // If Shizuku is enabled and available, we can launch automatically
        if (settings.isShizukuEnabled && shizukuManager.hasPermission() && settings.launchAutomatically) {
            // Trigger session launch via ShizukuManager
            shizukuManager.launchAppToInbox()
        } else {
            // If not available, we can't launch automatically but UI can still react when app is opened
        }
    }
    
    suspend fun incrementSnoozeCount() {
        settingsRepository.incrementSnoozeCount()
    }
}
