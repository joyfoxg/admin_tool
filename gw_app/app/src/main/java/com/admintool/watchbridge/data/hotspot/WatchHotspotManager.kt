package com.admintool.watchbridge.data.hotspot

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
private const val STARTUP_TIMEOUT_MS = 10_000L // 10초 타임아웃

@Singleton
class WatchHotspotManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val connectivityManager: ConnectivityManager by lazy {
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val p2pManager: WifiP2pManager? by lazy {
        context.applicationContext.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    private var p2pChannel: WifiP2pManager.Channel? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _hotspotStatus = MutableStateFlow<HotspotStatus>(HotspotStatus.Stopped)
    val hotspotStatus: StateFlow<HotspotStatus> = _hotspotStatus.asStateFlow()

    private var isP2pMode = false
    private var receiverRegistered = false
    private var startupTimeoutRunnable: Runnable? = null

    private val p2pReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            Log.d(TAG, "P2P Broadcast received: $action")

            when (action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    Log.d(TAG, "WIFI_P2P_STATE_CHANGED: state=$state")
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    @Suppress("DEPRECATION")
                    val p2pInfo = intent.getParcelableExtra<WifiP2pInfo>(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
                    @Suppress("DEPRECATION")
                    val group = intent.getParcelableExtra<WifiP2pGroup>(WifiP2pManager.EXTRA_WIFI_P2P_GROUP)

                    Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED: groupFormed=${p2pInfo?.groupFormed}")

                    if (group != null && (p2pInfo?.isGroupOwner == true || group.isGroupOwner)) {
                        val ssid = group.networkName ?: "DIRECT-Watch-Shizuku"
                        val password = group.passphrase ?: ""
                        Log.i(TAG, "Group info received via broadcast: SSID=$ssid, Pass=$password")
                        cancelStartupTimeout()
                        _hotspotStatus.value = HotspotStatus.Running(
                            ssid = ssid,
                            password = password,
                            localIp = "192.168.49.1"
                        )
                    } else if (p2pInfo?.groupFormed == true && p2pManager != null && p2pChannel != null) {
                        requestGroupDetails()
                    }
                }
            }
        }
    }

    init {
        initP2pChannel()
        registerReceiver()
    }

    private fun registerReceiver() {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(p2pReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(p2pReceiver, filter)
            }
            receiverRegistered = true
        }
    }

    private fun initP2pChannel() {
        if (p2pManager != null && p2pChannel == null) {
            try {
                p2pChannel = p2pManager?.initialize(context, Looper.getMainLooper()) {
                    Log.w(TAG, "Wi-Fi Direct channel disconnected")
                    p2pChannel = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize WifiP2pChannel", e)
            }
        }
    }

    /**
     * ConnectivityManager 기반으로 실제 Wi-Fi STA(인프라) 연결 여부를 정확하게 판단.
     * - 블루투스 프록시 경유 연결은 TRANSPORT_WIFI가 아니므로 오탐 방지.
     * - deprecated WifiInfo.networkId 대신 NetworkCapabilities 사용.
     */
    fun isConnectedToExternalWifi(): Pair<Boolean, String?> {
        try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false to null
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false to null

            // TRANSPORT_WIFI가 있어야 실제 Wi-Fi AP에 연결된 것
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return false to null
            }

            // Wi-Fi 연결이 확인된 경우, SSID 가져오기 시도
            val ssid = try {
                @Suppress("DEPRECATION")
                val connectionInfo = wifiManager.connectionInfo
                connectionInfo?.ssid?.replace("\"", "")?.takeIf {
                    it.isNotEmpty() && it != "<unknown ssid>" && it != "0x"
                }
            } catch (e: Exception) {
                null
            }

            return true to (ssid ?: "알 수 없는 네트워크")
        } catch (e: Exception) {
            Log.w(TAG, "isConnectedToExternalWifi check failed", e)
            return false to null
        }
    }

    @SuppressLint("MissingPermission")
    fun startHotspot() {
        if (_hotspotStatus.value is HotspotStatus.Running || _hotspotStatus.value is HotspotStatus.Starting) {
            return
        }

        // [수정 3] Wi-Fi가 꺼져 있으면 자동으로 켜기 (P2P는 Wi-Fi 하드웨어 필요)
        if (!wifiManager.isWifiEnabled) {
            Log.i(TAG, "Wi-Fi is disabled. Enabling Wi-Fi for P2P...")
            @Suppress("DEPRECATION")
            val enabled = wifiManager.setWifiEnabled(true)
            if (!enabled) {
                _hotspotStatus.value = HotspotStatus.Failed(
                    "Wi-Fi가 꺼져 있습니다.\n워치 설정에서 Wi-Fi를 켜고 다시 시도해 주세요."
                )
                return
            }
            // Wi-Fi가 완전히 켜질 때까지 약간 대기 후 시작
            _hotspotStatus.value = HotspotStatus.Starting("Wi-Fi 활성화 중...")
            handler.postDelayed({
                proceedStartHotspot()
            }, 1500)
            return
        }

        proceedStartHotspot()
    }

    private fun proceedStartHotspot() {
        // [수정 1] ConnectivityManager 기반 외부 Wi-Fi 감지
        val (isConnected, ssid) = isConnectedToExternalWifi()
        if (isConnected && ssid != null) {
            _hotspotStatus.value = HotspotStatus.Failed(
                "현재 공유기($ssid)에 연결되어 있습니다.\n워치 설정 > Wi-Fi에서 연결을 [해제] 후 시작해 주세요."
            )
            return
        }

        _hotspotStatus.value = HotspotStatus.Starting("브릿지 AP 생성 중...")
        Log.i(TAG, "Starting Hotspot Bridge...")

        // [수정 5] 전체 플로우 10초 타임아웃 설정
        scheduleStartupTimeout()

        // [수정 2] 기존 P2P 그룹 정리 후 새 그룹 생성
        cleanupAndStartWifiDirect()
    }

    /**
     * [수정 5] 타임아웃: 10초 내에 Running 상태가 되지 않으면 실패 처리
     */
    private fun scheduleStartupTimeout() {
        cancelStartupTimeout()
        startupTimeoutRunnable = Runnable {
            if (_hotspotStatus.value is HotspotStatus.Starting) {
                Log.e(TAG, "Startup timeout reached (${STARTUP_TIMEOUT_MS}ms). Aborting.")
                _hotspotStatus.value = HotspotStatus.Failed(
                    "핫스팟 시작 시간 초과 (10초)\n다시 시도해 주세요."
                )
                // 남은 P2P 리소스 정리
                cleanupP2pSilently()
            }
        }
        handler.postDelayed(startupTimeoutRunnable!!, STARTUP_TIMEOUT_MS)
    }

    private fun cancelStartupTimeout() {
        startupTimeoutRunnable?.let { handler.removeCallbacks(it) }
        startupTimeoutRunnable = null
    }

    /**
     * [수정 2] 기존 P2P 그룹을 먼저 제거한 후 새 그룹 생성.
     * 이전 그룹이 남아 있으면 createGroup이 BUSY(2)로 실패하는 문제 방지.
     */
    @SuppressLint("MissingPermission")
    private fun cleanupAndStartWifiDirect() {
        initP2pChannel()
        val channel = p2pChannel
        val manager = p2pManager

        if (manager == null || channel == null) {
            Log.e(TAG, "WifiP2pManager unavailable on this device")
            cancelStartupTimeout()
            _hotspotStatus.value = HotspotStatus.Failed(
                "이 워치에서는 Wi-Fi Direct를 사용할 수 없습니다."
            )
            return
        }

        // 기존 그룹이 있으면 제거 후 생성, 없으면 바로 생성
        manager.requestGroupInfo(channel) { existingGroup ->
            if (existingGroup != null) {
                Log.i(TAG, "Existing P2P group found (${existingGroup.networkName}). Removing first...")
                manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d(TAG, "Old group removed. Starting new group after delay...")
                        handler.postDelayed({
                            startWifiDirectGroup()
                        }, 500)
                    }

                    override fun onFailure(reason: Int) {
                        Log.w(TAG, "Old group removal failed ($reason). Trying to create anyway...")
                        startWifiDirectGroup()
                    }
                })
            } else {
                startWifiDirectGroup()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startWifiDirectGroup() {
        val channel = p2pChannel
        val manager = p2pManager

        if (manager == null || channel == null) {
            cancelStartupTimeout()
            _hotspotStatus.value = HotspotStatus.Failed(
                "Wi-Fi Direct 초기화 실패.\n앱을 재시작해 주세요."
            )
            return
        }

        // Trigger peer discovery first to wake up P2P engine from disabled/idle state
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "discoverPeers success (P2P engine awake). Creating group...")
                createGroupWithConfig(manager, channel)
            }

            override fun onFailure(reason: Int) {
                Log.w(TAG, "discoverPeers returned $reason. Proceeding to create group directly...")
                createGroupWithConfig(manager, channel)
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun createGroupWithConfig(manager: WifiP2pManager, channel: WifiP2pManager.Channel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val randomSuffix = (1000..9999).random()
                val config = WifiP2pConfig.Builder()
                    .setNetworkName("DIRECT-Watch-$randomSuffix")
                    .setPassphrase("12345678")
                    .build()

                manager.createGroup(channel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.i(TAG, "createGroup with custom config success!")
                        isP2pMode = true
                        cancelStartupTimeout()
                        _hotspotStatus.value = HotspotStatus.Running(
                            ssid = "DIRECT-Watch-$randomSuffix",
                            password = "12345678",
                            localIp = "192.168.49.1"
                        )
                        handler.postDelayed({ requestGroupDetails() }, 600)
                    }

                    override fun onFailure(reason: Int) {
                        Log.w(TAG, "createGroup with config failed ($reason). Trying legacy createGroup...")
                        createLegacyGroup(manager, channel)
                    }
                })
                return
            } catch (e: Exception) {
                Log.w(TAG, "createGroup config builder error: ${e.message}")
            }
        }

        createLegacyGroup(manager, channel)
    }

    @SuppressLint("MissingPermission")
    private fun createLegacyGroup(manager: WifiP2pManager, channel: WifiP2pManager.Channel) {
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Legacy createGroup success! Requesting details...")
                isP2pMode = true
                handler.postDelayed({ requestGroupDetails() }, 500)
            }

            override fun onFailure(reason: Int) {
                // [수정 4] LocalOnlyHotspot fallback 제거 — Galaxy Watch에서는 항상 실패하므로
                // 의미 있는 에러 메시지를 직접 표시
                Log.e(TAG, "All createGroup attempts failed (reason: $reason)")
                cancelStartupTimeout()
                val reasonStr = when (reason) {
                    WifiP2pManager.P2P_UNSUPPORTED -> "이 워치는 Wi-Fi Direct를 지원하지 않습니다."
                    WifiP2pManager.BUSY -> "Wi-Fi가 다른 작업 중입니다.\n잠시 후 다시 시도해 주세요."
                    WifiP2pManager.ERROR -> "Wi-Fi Direct 오류가 발생했습니다.\nWi-Fi를 껐다 켜고 다시 시도해 주세요."
                    else -> "핫스팟 생성 실패 (코드: $reason)\nWi-Fi를 껐다 켜고 다시 시도해 주세요."
                }
                _hotspotStatus.value = HotspotStatus.Failed(reasonStr)
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun requestGroupDetails() {
        val manager = p2pManager ?: return
        val channel = p2pChannel ?: return

        manager.requestGroupInfo(channel) { group ->
            if (group != null) {
                val ssid = group.networkName ?: "DIRECT-Watch-Shizuku"
                val password = group.passphrase ?: ""
                Log.i(TAG, "Group active: SSID=$ssid, Passphrase=$password")

                cancelStartupTimeout()
                _hotspotStatus.value = HotspotStatus.Running(
                    ssid = ssid,
                    password = password,
                    localIp = "192.168.49.1"
                )
            }
        }
    }

    /**
     * P2P 리소스를 조용히 정리 (에러 시 사용)
     */
    @SuppressLint("MissingPermission")
    private fun cleanupP2pSilently() {
        try {
            val channel = p2pChannel ?: return
            val manager = p2pManager ?: return
            manager.removeGroup(channel, null)
        } catch (e: Exception) {
            Log.w(TAG, "Silent P2P cleanup error: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopHotspot() {
        Log.i(TAG, "Stopping Hotspot Bridge...")
        cancelStartupTimeout()

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

        _hotspotStatus.value = HotspotStatus.Stopped
    }
}
