package com.admintool.deviceadmin.data.repository

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.admintool.deviceadmin.data.model.AdminPolicyInfo
import com.admintool.deviceadmin.data.model.DeviceAdminApp
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceAdminRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuExecutor: ShizukuShellExecutor,
    private val standardExecutor: StandardShellExecutor,
    private val permissionRepository: PermissionRepository
) : DeviceAdminRepository {

    private val dpm: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }
    private val pm: PackageManager by lazy {
        context.packageManager
    }

    private val criticalSystemPackages = setOf(
        "com.google.android.gms",
        "com.google.android.apps.adm",
        "com.samsung.android.fmm",
        "com.sec.android.app.find",
        "com.android.settings"
    )

    override fun getInstalledDeviceAdmins(): Flow<List<DeviceAdminApp>> = flow {
        val activeAdmins = dpm.activeAdmins ?: emptyList()
        val activeSet = activeAdmins.map { it.flattenToShortString() }.toSet()

        val intent = Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED)
        val resolveInfos = pm.queryBroadcastReceivers(
            intent,
            PackageManager.GET_META_DATA or PackageManager.MATCH_DISABLED_COMPONENTS
        )

        val resultList = mutableListOf<DeviceAdminApp>()

        for (resolveInfo in resolveInfos) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val packageName = activityInfo.packageName
            val className = activityInfo.name
            val componentName = ComponentName(packageName, className)

            val appInfo = try {
                pm.getApplicationInfo(packageName, 0)
            } catch (e: Exception) {
                null
            }

            val appName = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: activityInfo.loadLabel(pm).toString()
            val icon = appInfo?.let { pm.getApplicationIcon(it) } ?: activityInfo.loadIcon(pm)

            val isSystemApp = (appInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) ?: 0) != 0
            val isActive = activeSet.contains(componentName.flattenToShortString()) ||
                    activeAdmins.any { it.packageName == packageName && it.className == className }

            val isCritical = criticalSystemPackages.contains(packageName) || (isSystemApp && packageName.contains("google"))

            val isDeviceOwner = try {
                dpm.isDeviceOwnerApp(packageName)
            } catch (e: Exception) {
                false
            }

            val isProfileOwner = try {
                dpm.isProfileOwnerApp(packageName)
            } catch (e: Exception) {
                false
            }

            val isUninstallBlocked = try {
                dpm.isUninstallBlocked(null, packageName)
            } catch (e: Exception) {
                false
            }

            val isGreyedOut = isDeviceOwner || isProfileOwner || isUninstallBlocked || (isActive && isCritical)

            val permissions = try {
                permissionRepository.getAppPermissions(packageName)
            } catch (e: Exception) {
                emptyList()
            }

            val hasOverlay = permissions.any { it.permission.contains("SYSTEM_ALERT_WINDOW") && it.isGranted }
            val hasAccessibility = permissions.any { it.permission.contains("BIND_ACCESSIBILITY_SERVICE") && it.isGranted }

            val policies = listOf(
                AdminPolicyInfo("WIPE_DATA", "원격 데이터 삭제"),
                AdminPolicyInfo("RESET_PASSWORD", "화면 잠금 비밀번호 재설정"),
                AdminPolicyInfo("LIMIT_PASSWORD", "비밀번호 정책 강제"),
                AdminPolicyInfo("FORCE_LOCK", "화면 즉시 강제 잠금"),
                AdminPolicyInfo("DISABLE_CAMERA", "카메라 사용 제한"),
                AdminPolicyInfo("ENCRYPTED_STORAGE", "저장소 암호화 강제")
            )

            resultList.add(
                DeviceAdminApp(
                    packageName = packageName,
                    className = className,
                    componentName = componentName,
                    appName = appName,
                    icon = icon,
                    isActiveAdmin = isActive,
                    isSystemApp = isSystemApp,
                    isCriticalSystemAdmin = isCritical,
                    isGreyedOutOrLocked = isGreyedOut,
                    isDeviceOrProfileOwner = isDeviceOwner || isProfileOwner,
                    requestedPolicies = policies,
                    permissions = permissions,
                    hasOverlayPermission = hasOverlay,
                    hasAccessibilityPermission = hasAccessibility
                )
            )
        }

        val sortedList = resultList.sortedWith(
            compareByDescending<DeviceAdminApp> { it.isActiveAdmin }
                .thenByDescending { it.isGreyedOutOrLocked }
                .thenBy { it.isSystemApp }
                .thenBy { it.appName }
        )
        emit(sortedList)
    }.flowOn(Dispatchers.IO)

    override suspend fun getPrivilegeMode(): PrivilegeMode {
        return if (shizukuExecutor.isAvailable()) {
            PrivilegeMode.SHIZUKU
        } else {
            PrivilegeMode.STANDARD
        }
    }

    private fun getActiveExecutor(): ShellExecutor {
        return if (shizukuExecutor.isAvailable()) {
            shizukuExecutor
        } else {
            standardExecutor
        }
    }

    override suspend fun removeDeviceAdminStandard(componentName: ComponentName): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dpm.removeActiveAdmin(componentName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun forceDeactivateAdmin(componentName: ComponentName, packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val executor = getActiveExecutor()
        if (executor.mode == PrivilegeMode.STANDARD) {
            return@withContext try {
                dpm.removeActiveAdmin(componentName)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("설정에서 잠긴 앱은 Shizuku(무선 ADB) 연동을 통해 강제 해제해야 합니다: ${e.message}"))
            }
        }

        try {
            val shortComponent = componentName.flattenToShortString()
            val fullComponent = "${componentName.packageName}/${componentName.className}"

            // 1. Force stop target app
            executor.execute("am force-stop $packageName")

            // 2. Disable overlay and accessibility locks
            executor.execute("appops set $packageName SYSTEM_ALERT_WINDOW ignore")
            executor.execute("pm revoke $packageName android.permission.BIND_ACCESSIBILITY_SERVICE")

            // 3. Clear profile/device owner status if applicable
            executor.execute("dpm clear-profile-owner --user 0 $shortComponent")
            executor.execute("dpm clear-device-owner $shortComponent")

            // 4. Try direct dpm remove-active-admin first
            val dpmResult = executor.execute("dpm remove-active-admin $shortComponent")
            if (dpmResult.isSuccess) {
                return@withContext Result.success(Unit)
            }

            // 5. Bypass "Attempt to remove non-test admin" security check via Package State Reset:
            // Disabling the package temporarily forces Android DevicePolicyManagerService to strip active admin bindings immediately.
            val disableResult = executor.execute("pm disable-user --user 0 $packageName")
            if (disableResult.isSuccess || disableResult.stdout.contains("disabled") || disableResult.stdout.contains("new state")) {
                // Immediately re-enable package so app stays normally usable for the user without device admin rights
                executor.execute("pm enable --user 0 $packageName")
                return@withContext Result.success(Unit)
            }

            // Fallback 6: Try pm disable / enable without --user flag
            val disableFallback = executor.execute("pm disable $packageName")
            executor.execute("pm enable $packageName")
            if (disableFallback.isSuccess || disableFallback.stdout.contains("disabled")) {
                return@withContext Result.success(Unit)
            }

            // Fallback 7: Try cmd device_policy
            val cmdPolicyResult = executor.execute("cmd device_policy remove-active-admin $shortComponent")
            if (cmdPolicyResult.isSuccess) {
                return@withContext Result.success(Unit)
            }

            Result.failure(Exception("강제 해제 실패: ${disableResult.stderr.ifEmpty { disableResult.stdout.ifEmpty { dpmResult.stderr } }}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun activateAdmin(componentName: ComponentName): Result<Unit> = withContext(Dispatchers.IO) {
        val executor = getActiveExecutor()
        if (executor.mode == PrivilegeMode.SHIZUKU) {
            val shortComponent = componentName.flattenToShortString()
            val fullComponent = "${componentName.packageName}/${componentName.className}"
            var res = executor.execute("dpm set-active-admin --user 0 $shortComponent")
            if (!res.isSuccess) {
                res = executor.execute("dpm set-active-admin --user 0 $fullComponent")
            }
            if (res.isSuccess) {
                return@withContext Result.success(Unit)
            }
        }

        // Fallback / Standard mode: Launch system Add Device Admin screen
        withContext(Dispatchers.Main) {
            try {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "기기 관리자 활성화 요청")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
