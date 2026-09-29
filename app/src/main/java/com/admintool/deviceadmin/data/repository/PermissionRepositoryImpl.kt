package com.admintool.deviceadmin.data.repository

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.admintool.deviceadmin.data.model.AppPermission
import com.admintool.deviceadmin.data.model.InstalledAppInfo
import com.admintool.deviceadmin.data.model.PrivilegeMode
import com.admintool.deviceadmin.data.shell.ShellExecutor
import com.admintool.deviceadmin.data.shell.ShizukuShellExecutor
import com.admintool.deviceadmin.data.shell.StandardShellExecutor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuExecutor: ShizukuShellExecutor,
    private val standardExecutor: StandardShellExecutor
) : PermissionRepository {

    private val pm: PackageManager by lazy { context.packageManager }
    private val appOps: AppOpsManager by lazy {
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    }
    private val dpm: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private fun getActiveExecutor(): ShellExecutor {
        return if (shizukuExecutor.isAvailable()) {
            shizukuExecutor
        } else {
            standardExecutor
        }
    }

    override fun getInstalledGeneralApps(): Flow<List<InstalledAppInfo>> = flow {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val activeAdmins = dpm.activeAdmins ?: emptyList()
        val activeAdminPkgs = activeAdmins.map { it.packageName }.toSet()

        val packages: List<PackageInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
        } catch (e: Exception) {
            emptyList()
        }

        val appList = mutableListOf<InstalledAppInfo>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val packageName = pkg.packageName

            // Skip self or internal framework-res
            if (packageName == context.packageName || packageName == "android") continue

            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }
            val icon = try {
                pm.getApplicationIcon(appInfo)
            } catch (e: Exception) {
                null
            }

            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isDeviceAdmin = activeAdminPkgs.contains(packageName)

            val permissions = getAppPermissions(packageName)
            val hasOverlay = permissions.any { it.permission.contains("SYSTEM_ALERT_WINDOW") && it.isGranted }
            val hasAccessibility = permissions.any { it.permission.contains("BIND_ACCESSIBILITY_SERVICE") && it.isGranted }
            val dangerousCount = permissions.count { it.isGranted && (it.isRuntimePermission || it.isSpecialAccess) }

            val installTime = dateFormat.format(Date(pkg.firstInstallTime))
            val updateTime = dateFormat.format(Date(pkg.lastUpdateTime))
            val targetSdk = appInfo.targetSdkVersion
            val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else 21
            val sourceDir = appInfo.sourceDir ?: ""

            appList.add(
                InstalledAppInfo(
                    packageName = packageName,
                    appName = appName,
                    icon = icon,
                    versionName = pkg.versionName ?: "1.0",
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg.longVersionCode else @Suppress("DEPRECATION") pkg.versionCode.toLong(),
                    targetSdkVersion = targetSdk,
                    minSdkVersion = minSdk,
                    isSystemApp = isSystemApp,
                    isDeviceAdmin = isDeviceAdmin,
                    installTimeFormatted = installTime,
                    lastUpdateTimeFormatted = updateTime,
                    sourceDir = sourceDir,
                    permissions = permissions,
                    hasOverlayPermission = hasOverlay,
                    hasAccessibilityPermission = hasAccessibility,
                    dangerousPermissionCount = dangerousCount
                )
            )
        }

        // Sort: User apps first, then by dangerous permissions count descending, then alphabetically
        val sorted = appList.sortedWith(
            compareBy<InstalledAppInfo> { it.isSystemApp }
                .thenByDescending { it.dangerousPermissionCount }
                .thenBy { it.appName }
        )
        emit(sorted)
    }.flowOn(Dispatchers.IO)

    override suspend fun getAppPermissions(packageName: String): List<AppPermission> = withContext(Dispatchers.IO) {
        val permissionsList = mutableListOf<AppPermission>()
        try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }

            val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
            val flags = packageInfo.requestedPermissionsFlags ?: IntArray(0)

            for (i in requestedPermissions.indices) {
                val perm = requestedPermissions[i]
                val isGranted = if (i < flags.size) {
                    (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                } else {
                    false
                }

                val isRuntime = isRuntimePermission(perm)
                val isSpecial = isSpecialAccessPermission(perm)
                val label = getPermissionLabel(perm)
                val description = getPermissionDescription(perm)
                val appOpName = getAppOpForPermission(perm)

                if (isRuntime || isSpecial || isSensitivePermission(perm)) {
                    permissionsList.add(
                        AppPermission(
                            permission = perm,
                            label = label,
                            description = description,
                            isGranted = isGranted,
                            isRuntimePermission = isRuntime,
                            isSpecialAccess = isSpecial,
                            appOpName = appOpName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Package might not be accessible
        }

        // Check SYSTEM_ALERT_WINDOW explicitly
        val hasOverlay = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    appInfo.uid,
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    appInfo.uid,
                    packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }

        if (permissionsList.none { it.permission == android.Manifest.permission.SYSTEM_ALERT_WINDOW }) {
            permissionsList.add(
                AppPermission(
                    permission = android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                    label = "다른 앱 위에 표시 (오버레이)",
                    description = "화면 상단에 팝업을 띄워 터치를 가로채거나 화면을 가릴 수 있습니다.",
                    isGranted = hasOverlay,
                    isRuntimePermission = false,
                    isSpecialAccess = true,
                    appOpName = "SYSTEM_ALERT_WINDOW"
                )
            )
        }

        permissionsList.sortedWith(
            compareByDescending<AppPermission> { it.isSpecialAccess }
                .thenByDescending { it.isRuntimePermission }
                .thenBy { it.label }
        )
    }

    override suspend fun togglePermission(
        packageName: String,
        permission: AppPermission,
        grant: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val executor = getActiveExecutor()

        if (executor.mode == PrivilegeMode.STANDARD) {
            return@withContext withContext(Dispatchers.Main) {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }

        try {
            if (permission.appOpName != null) {
                val modeStr = if (grant) "allow" else "ignore"
                val result = executor.execute("appops set $packageName ${permission.appOpName} $modeStr")
                if (!result.isSuccess && result.stderr.isNotEmpty()) {
                    return@withContext Result.failure(Exception(result.stderr))
                }
            }

            if (permission.isRuntimePermission) {
                val action = if (grant) "grant" else "revoke"
                val result = executor.execute("pm $action $packageName ${permission.permission}")
                if (!result.isSuccess && result.stderr.isNotEmpty()) {
                    return@withContext Result.failure(Exception(result.stderr))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun forceStopApp(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val executor = getActiveExecutor()
        if (executor.mode == PrivilegeMode.STANDARD) {
            return@withContext Result.failure(Exception("Shizuku (무선 ADB) 권한이 필요합니다."))
        }
        val result = executor.execute("am force-stop $packageName")
        if (result.isSuccess) Result.success(Unit) else Result.failure(Exception(result.stderr))
    }

    override suspend fun revokeOverlayPermission(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val executor = getActiveExecutor()
        if (executor.mode == PrivilegeMode.STANDARD) {
            return@withContext Result.failure(Exception("Shizuku (무선 ADB) 권한이 필요합니다."))
        }
        val result = executor.execute("appops set $packageName SYSTEM_ALERT_WINDOW ignore")
        if (result.isSuccess) Result.success(Unit) else Result.failure(Exception(result.stderr))
    }

    override suspend fun revokeAccessibilityPermission(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val executor = getActiveExecutor()
        if (executor.mode == PrivilegeMode.STANDARD) {
            return@withContext Result.failure(Exception("Shizuku (무선 ADB) 권한이 필요합니다."))
        }
        executor.execute("pm revoke $packageName android.permission.BIND_ACCESSIBILITY_SERVICE")
        Result.success(Unit)
    }

    override suspend fun uninstallApp(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val executor = getActiveExecutor()
        if (executor.mode == PrivilegeMode.SHIZUKU) {
            val result = executor.execute("pm uninstall --user 0 $packageName")
            if (result.isSuccess || result.stdout.contains("Success")) {
                return@withContext Result.success(Unit)
            }
        }

        // Standard fallback
        withContext(Dispatchers.Main) {
            try {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.fromParts("package", packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun isRuntimePermission(perm: String): Boolean {
        return perm.startsWith("android.permission.ACCESS_") ||
                perm.startsWith("android.permission.CAMERA") ||
                perm.startsWith("android.permission.RECORD_AUDIO") ||
                perm.startsWith("android.permission.READ_") ||
                perm.startsWith("android.permission.WRITE_") ||
                perm.startsWith("android.permission.POST_NOTIFICATIONS") ||
                perm.startsWith("android.permission.BODY_SENSORS")
    }

    private fun isSpecialAccessPermission(perm: String): Boolean {
        return perm.contains("SYSTEM_ALERT_WINDOW") ||
                perm.contains("BIND_ACCESSIBILITY_SERVICE") ||
                perm.contains("PACKAGE_USAGE_STATS") ||
                perm.contains("MANAGE_EXTERNAL_STORAGE") ||
                perm.contains("REQUEST_INSTALL_PACKAGES")
    }

    private fun isSensitivePermission(perm: String): Boolean {
        return perm.contains("DEVICE_ADMIN") ||
                perm.contains("RECEIVE_BOOT_COMPLETED") ||
                perm.contains("FOREGROUND_SERVICE")
    }

    private fun getPermissionLabel(perm: String): String {
        return when {
            perm.endsWith("SYSTEM_ALERT_WINDOW") -> "다른 앱 위에 표시 (오버레이)"
            perm.endsWith("BIND_ACCESSIBILITY_SERVICE") -> "접근성 서비스 제어"
            perm.endsWith("POST_NOTIFICATIONS") -> "알림 전송"
            perm.endsWith("CAMERA") -> "카메라 촬영"
            perm.endsWith("RECORD_AUDIO") -> "오디오/마이크 녹음"
            perm.endsWith("ACCESS_FINE_LOCATION") -> "정확한 위치 정보"
            perm.endsWith("ACCESS_COARSE_LOCATION") -> "대략적인 위치 정보"
            perm.endsWith("ACCESS_BACKGROUND_LOCATION") -> "백그라운드 위치 추적"
            perm.endsWith("READ_EXTERNAL_STORAGE") -> "외부 저장소 읽기"
            perm.endsWith("WRITE_EXTERNAL_STORAGE") -> "외부 저장소 쓰기"
            perm.endsWith("MANAGE_EXTERNAL_STORAGE") -> "모든 파일 관리 권한"
            perm.endsWith("READ_MEDIA_IMAGES") -> "사진 및 이미지 접근"
            perm.endsWith("READ_MEDIA_VIDEO") -> "동영상 파일 접근"
            perm.endsWith("READ_MEDIA_AUDIO") -> "음악 및 오디오 접근"
            perm.endsWith("READ_CONTACTS") -> "연락처 읽기"
            perm.endsWith("READ_CALL_LOG") -> "통화 기록 조회"
            perm.endsWith("READ_SMS") -> "SMS 문자 읽기"
            perm.endsWith("RECEIVE_BOOT_COMPLETED") -> "부팅 시 자동 시작"
            else -> perm.substringAfterLast(".")
        }
    }

    private fun getPermissionDescription(perm: String): String {
        return when {
            perm.endsWith("SYSTEM_ALERT_WINDOW") -> "화면을 가리거나 터치를 방해하는 오버레이 창을 표시할 수 있습니다."
            perm.endsWith("BIND_ACCESSIBILITY_SERVICE") -> "화면의 텍스트를 읽거나 사용자 대신 버튼을 클릭할 수 있습니다."
            perm.endsWith("ACCESS_FINE_LOCATION") -> "기기의 실시간 GPS 좌표를 수집합니다."
            perm.endsWith("CAMERA") -> "사용자 모르게 카메라를 활성화할 수 있습니다."
            perm.endsWith("RECORD_AUDIO") -> "마이크를 통해 주변 소리를 녹음할 수 있습니다."
            else -> "앱이 시스템 리소스 및 기능에 접근할 수 있도록 허용합니다."
        }
    }

    private fun getAppOpForPermission(perm: String): String? {
        return when {
            perm.endsWith("SYSTEM_ALERT_WINDOW") -> "SYSTEM_ALERT_WINDOW"
            perm.endsWith("POST_NOTIFICATIONS") -> "POST_NOTIFICATION"
            perm.endsWith("ACCESS_FINE_LOCATION") -> "FINE_LOCATION"
            perm.endsWith("CAMERA") -> "CAMERA"
            perm.endsWith("RECORD_AUDIO") -> "RECORD_AUDIO"
            perm.endsWith("MANAGE_EXTERNAL_STORAGE") -> "MANAGE_EXTERNAL_STORAGE"
            else -> null
        }
    }
}
