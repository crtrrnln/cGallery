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

    private var lastLaunchTime = 0L

    fun start() {
        scope.launch {
            inboxDao.getPendingItems().collectLatest { items ->
                val detectedOnly = items.filter { it.status == InboxStatus.Detected }
                if (detectedOnly.isNotEmpty()) {
                    checkAndTriggerSession(detectedOnly)
                }
            }
        }
    }

    private suspend fun checkAndTriggerSession(items: List<InboxItemEntity>) {
        if (System.currentTimeMillis() - lastLaunchTime < 5000) return
        val settings = settingsRepository.settingsFlow.first()
        
        if (!settings.isEnforcementEnabled) return
        
        if (settings.snoozeExpirationTime > System.currentTimeMillis()) {
            return
        }
        
        if (settings.snoozeItemThreshold > 0 && items.size < settings.snoozeItemThreshold) {
            return
        }

        if (settings.isShizukuEnabled && shizukuManager.hasPermission() && settings.launchAutomatically) {
            shizukuManager.launchAppToInbox()
            lastLaunchTime = System.currentTimeMillis()
        }
    }
    
    suspend fun incrementSnoozeCount() {
        settingsRepository.incrementSnoozeCount()
    }
}
