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
        
        if (settings.snoozeExpirationTime > System.currentTimeMillis()) {
            return
        }
        
        if (settings.snoozeItemThreshold > 0 && settings.currentSnoozeCount < settings.snoozeItemThreshold) {
            return
        }

        if (settings.isShizukuEnabled && shizukuManager.hasPermission() && settings.launchAutomatically) {
            shizukuManager.launchAppToInbox()
        }
    }
    
    suspend fun incrementSnoozeCount() {
        settingsRepository.incrementSnoozeCount()
    }
}
