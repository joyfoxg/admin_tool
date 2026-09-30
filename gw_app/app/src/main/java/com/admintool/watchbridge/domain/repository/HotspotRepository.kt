package com.admintool.watchbridge.domain.repository

import com.admintool.watchbridge.data.model.HotspotStatus
import kotlinx.coroutines.flow.StateFlow

interface HotspotRepository {
    val hotspotStatus: StateFlow<HotspotStatus>
    fun startHotspot()
    fun stopHotspot()
}
