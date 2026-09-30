package com.admintool.watchbridge.data.hotspot

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
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
    private var receiverRegistered = false

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
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    val p2pInfo = intent.getParcelableExtra<WifiP2pInfo>(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
                    val group = intent.getParcelableExtra<WifiP2pGroup>(WifiP2pManager.EXTRA_WIFI_P2P_GROUP)

                    Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED: isConnected=${networkInfo?.isConnected}, groupFormed=${p2pInfo?.groupFormed}")

                    if (group != null && (p2pInfo?.isGroupOwner == true || group.isGroupOwner)) {
                        val ssid = group.networkName ?: "DIRECT-Watch-Shizuku"
                        val password = group.passphrase ?: ""
                        Log.i(TAG, "Group info received via broadcast: SSID=$ssid, Pass=$password")
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

    fun isConnectedToExternalWifi(): Pair<Boolean, String?> {
        try {
            val connectionInfo: WifiInfo? = wifiManager.connectionInfo
            if (connectionInfo != null && connectionInfo.networkId != -1) {
                val ssid = connectionInfo.ssid?.replace("\"", "") ?: ""
                if (ssid.isNotEmpty() && ssid != "<unknown ssid>" && ssid != "0x") {
                    return true to ssid
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return false to null
    }

    @SuppressLint("MissingPermission")
    fun startHotspot() {
        if (_hotspotStatus.value is HotspotStatus.Running || _hotspotStatus.value is HotspotStatus.Starting) {
            return
        }

        // Check if Wi-Fi is currently connected to an external router
        val (isConnected, ssid) = isConnectedToExternalWifi()
        if (isConnected && ssid != null) {
            _hotspotStatus.value = HotspotStatus.Failed(
                "현재 공유기($ssid)에 연결되어 있습니다.\n워치 설정 > Wi-Fi에서 연결을 [해제] 후 시작해 주세요."
            )
            return
        }

        _hotspotStatus.value = HotspotStatus.Starting("브릿지 AP 생성 중...")
        Log.i(TAG, "Starting Hotspot Bridge...")

        startWifiDirectGroup()
    }

    @SuppressLint("MissingPermission")
    private fun startWifiDirectGroup() {
        initP2pChannel()
        val channel = p2pChannel
        val manager = p2pManager

        if (manager == null || channel == null) {
            Log.w(TAG, "WifiP2pManager unavailable, trying LocalOnlyHotspot")
            startLocalOnlyHotspotFallback()
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
                Log.w(TAG, "Legacy createGroup failed (reason: $reason). Falling back to LocalOnlyHotspot...")
                startLocalOnlyHotspotFallback()
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

                _hotspotStatus.value = HotspotStatus.Running(
                    ssid = ssid,
                    password = password,
                    localIp = "192.168.49.1"
                )
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
                        ERROR_GENERIC -> "핫스팟 시작 실패: 워치 Wi-Fi가 켜져 있는지 확인하세요."
                        ERROR_INCOMPATIBLE_MODE -> "현재 Wi-Fi 모드와 호환되지 않습니다."
                        ERROR_TETHERING_DISALLOWED -> "기기에서 핫스팟이 제한되었습니다."
                        else -> "핫스팟 오류 (Code: $reason)"
                    }
                    _hotspotStatus.value = HotspotStatus.Failed(reasonStr)
                }
            }, handler)
        } catch (e: SecurityException) {
            _hotspotStatus.value = HotspotStatus.Failed("권한 오류: 위치 권한을 확인하세요.")
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
