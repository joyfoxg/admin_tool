package com.admintool.watchbridge.data.model

sealed interface HotspotStatus {
    data object Stopped : HotspotStatus
    data class Starting(val message: String = "브릿지 모드 준비 중...") : HotspotStatus
    data class Running(
        val ssid: String,
        val password: String,
        val localIp: String = "192.168.49.1",
        val remainingSeconds: Int? = null
    ) : HotspotStatus
    data class Failed(val reason: String) : HotspotStatus
}

data class HotspotConfig(
    val autoTimeoutMinutes: Int = 5, // 0 means unlimited
    val keepScreenOn: Boolean = true
)
