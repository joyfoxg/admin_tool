package com.admintool.watchbridge.data.hotspot

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.admintool.watchbridge.data.model.HotspotStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHotspotManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _hotspotStatus = MutableStateFlow<HotspotStatus>(HotspotStatus.Stopped)
    val hotspotStatus: StateFlow<HotspotStatus> = _hotspotStatus.asStateFlow()

    fun startHotspot() {
        if (_hotspotStatus.value is HotspotStatus.Running || _hotspotStatus.value is HotspotStatus.Starting) {
            return
        }

        _hotspotStatus.value = HotspotStatus.Starting

        try {
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                    super.onStarted(res)
                    reservation = res

                    var ssid = "Watch_Shizuku_Bridge"
                    var password = ""

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val config = res.softApConfiguration
                        if (config != null) {
                            ssid = config.ssid ?: ssid
                            password = config.passphrase ?: ""
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val config = res.wifiConfiguration
                        if (config != null) {
                            ssid = config.SSID ?: ssid
                            password = config.preSharedKey ?: ""
                        }
                    }

                    _hotspotStatus.value = HotspotStatus.Running(
                        ssid = ssid,
                        password = password,
                        localIp = "192.168.49.1"
                    )
                }

                override fun onStopped() {
                    super.onStopped()
                    reservation = null
                    _hotspotStatus.value = HotspotStatus.Stopped
                }

                override fun onFailed(reason: Int) {
                    super.onFailed(reason)
                    reservation = null
                    val reasonStr = when (reason) {
                        ERROR_NO_CHANNEL -> "사용 가능한 Wi-Fi 채널이 없습니다."
                        ERROR_GENERIC -> "워치 하드웨어 핫스팟 시작 실패 (Wi-Fi 상태를 확인하세요)."
                        ERROR_INCOMPATIBLE_MODE -> "현재 Wi-Fi 모드와 호환되지 않습니다."
                        ERROR_TETHERING_DISALLOWED -> "기기에서 핫스팟이 제한되었습니다."
                        else -> "핫스팟 오류 (Code: $reason)"
                    }
                    _hotspotStatus.value = HotspotStatus.Failed(reasonStr)
                }
            }, handler)
        } catch (e: SecurityException) {
            _hotspotStatus.value = HotspotStatus.Failed("권한 오류: 위치 또는 주변 기기 권한이 필요합니다.")
        } catch (e: Exception) {
            _hotspotStatus.value = HotspotStatus.Failed("시작 실패: ${e.message}")
        }
    }

    fun stopHotspot() {
        try {
            reservation?.close()
            reservation = null
        } catch (e: Exception) {
            // Ignore close errors
        } finally {
            _hotspotStatus.value = HotspotStatus.Stopped
        }
    }
}
