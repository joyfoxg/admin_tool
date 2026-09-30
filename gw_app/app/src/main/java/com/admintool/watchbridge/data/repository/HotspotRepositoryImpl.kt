package com.admintool.watchbridge.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.admintool.watchbridge.data.hotspot.WatchHotspotManager
import com.admintool.watchbridge.data.model.HotspotStatus
import com.admintool.watchbridge.domain.repository.HotspotRepository
import com.admintool.watchbridge.service.HotspotForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotspotRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hotspotManager: WatchHotspotManager
) : HotspotRepository {

    override val hotspotStatus: StateFlow<HotspotStatus> = hotspotManager.hotspotStatus

    override fun startHotspot() {
        val intent = Intent(context, HotspotForegroundService::class.java).apply {
            action = HotspotForegroundService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stopHotspot() {
        val intent = Intent(context, HotspotForegroundService::class.java).apply {
            action = HotspotForegroundService.ACTION_STOP
        }
        context.startService(intent)
        hotspotManager.stopHotspot()
    }
}
