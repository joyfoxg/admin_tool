package com.admintool.watchbridge.data.hotspot

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.admintool.watchbridge.data.model.HotspotStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WatchHotspotManager"

@Singleton
class WatchHotspotManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val p2pManager: WifiP2pManager? by lazy {
        context.applicationContext.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    private var p2pChannel: WifiP2pManager.Channel? = null
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _hotspotStatus = MutableStateFlow<HotspotStatus>(HotspotStatus.Stopped)
    val hotspotStatus: StateFlow<HotspotStatus> = _hotspotStatus.asStateFlow()

    private var isP2pMode = false

    init {
        initP2pChannel()
    }

    private fun initP2pChannel() {
        if (p2pManager != null && p2pChannel == null) {
            try {
                p2pChannel = p2pManager?.initialize(context, Looper.getMainLooper()) {
                    Log.w(TAG, "Wi-Fi Direct channel disconnected, re-initializing")
                    p2pChannel = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize WifiP2pChannel", e)
            }
        }
    }

    fun startHotspot() {
        if (_hotspotStatus.value is HotspotStatus.Running || _hotspotStatus.value is HotspotStatus.Starting) {
            return
        }

        _hotspotStatus.value = HotspotStatus.Starting
        Log.i(TAG, "Starting Hotspot Bridge...")

        // Step 1: Attempt Wi-Fi Direct Autonomous Group Owner (Best support on Galaxy Watch Wear OS)
        startWifiDirectGroup()
    }

    @SuppressLint("MissingPermission")
    private fun startWifiDirectGroup() {
        initP2pChannel()
        val channel = p2pChannel
        val manager = p2pManager

        if (manager == null || channel == null) {
            Log.w(TAG, "WifiP2pManager unavailable, falling back to LocalOnlyHotspot")
            startLocalOnlyHotspotFallback()
            return
        }

        // First remove any stale existing group before creating
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                createGroupInternal(manager, channel)
            }

            override fun onFailure(reason: Int) {
                // Ignore removal failure (often no group exists) and proceed to create
                createGroupInternal(manager, channel)
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun createGroupInternal(manager: WifiP2pManager, channel: WifiP2pManager.Channel) {
        // Try creating standard autonomous group
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "WifiP2p createGroup success! Requesting group details...")
                isP2pMode = true

                // Wait brief moment for interface/passphrase to populate
                handler.postDelayed({
                    fetchP2pGroupInfo(manager, channel)
                }, 500)
            }

            override fun onFailure(reason: Int) {
                Log.w(TAG, "WifiP2p createGroup failed (reason: $reason). Trying LocalOnlyHotspot fallback...")
                startLocalOnlyHotspotFallback()
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun fetchP2pGroupInfo(manager: WifiP2pManager, channel: WifiP2pManager.Channel) {
        manager.requestGroupInfo(channel) { group ->
            if (group != null) {
                val ssid = group.networkName ?: "DIRECT-Watch-Shizuku"
                val password = group.passphrase ?: ""

                Log.i(TAG, "P2P Group active: SSID=$ssid, Passphrase=$password")

                _hotspotStatus.value = HotspotStatus.Running(
                    ssid = ssid,
                    password = password,
                    localIp = "192.168.49.1"
                )
            } else {
                Log.w(TAG, "requestGroupInfo returned null, retrying once...")
                handler.postDelayed({
                    manager.requestGroupInfo(channel) { retryGroup ->
                        if (retryGroup != null) {
                            _hotspotStatus.value = HotspotStatus.Running(
                                ssid = retryGroup.networkName ?: "DIRECT-Watch-Shizuku",
                                password = retryGroup.passphrase ?: "",
                                localIp = "192.168.49.1"
                            )
                        } else {
                            _hotspotStatus.value = HotspotStatus.Failed("Wi-Fi Direct 그룹 정보 생성 실패")
                        }
                    }
                }, 1000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocalOnlyHotspotFallback() {
        Log.i(TAG, "Attempting startLocalOnlyHotspot...")
        try {
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                    super.onStarted(res)
                    reservation = res
                    isP2pMode = false

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
                        ERROR_GENERIC -> "워치 핫스팟/P2P 시작 실패 (Wi-Fi 켜짐 여부를 확인하세요)."
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

    @SuppressLint("MissingPermission")
    fun stopHotspot() {
        Log.i(TAG, "Stopping Hotspot Bridge...")

        if (isP2pMode) {
            val channel = p2pChannel
            val manager = p2pManager
            if (manager != null && channel != null) {
                manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.i(TAG, "WifiP2p group removed successfully")
                    }
                    override fun onFailure(reason: Int) {
                        Log.w(TAG, "WifiP2p group removal failed ($reason)")
                    }
                })
            }
            isP2pMode = false
        }

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
