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
    private val settingsRepository: AppSettingsRepository,
    private val shizukuManager: ShizukuManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val db = VirtualAlbumDatabase.getDatabase(context)
    private val inboxDao = db.inboxDao()
    private var lastLaunchTime = 0L

    fun start() {
        scope.launch {
            inboxDao.getPendingItems().collectLatest { items ->
                if (items.isNotEmpty()) checkAndTriggerSession(items)
            }
        }
    }

    private suspend fun checkAndTriggerSession(items: List<InboxItemEntity>) {
        if (System.currentTimeMillis() - lastLaunchTime < 10000) return
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.isEnforcementEnabled) return
        
        val isSnoozedTime = settings.snoozeExpirationTime > System.currentTimeMillis()
        val isSnoozedItems = settings.snoozeItemThreshold > 0 && items.size < settings.snoozeItemThreshold
        if (isSnoozedTime || isSnoozedItems) return

        if (settings.isShizukuEnabled && shizukuManager.hasPermission() && settings.launchAutomatically) {
            shizukuManager.launchAppToInbox()
            lastLaunchTime = System.currentTimeMillis()
        }
    }
}
