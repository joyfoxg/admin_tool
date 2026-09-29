package com.admintool.deviceadmin.data.repository

import com.admintool.deviceadmin.data.model.AppPermission
import com.admintool.deviceadmin.data.model.InstalledAppInfo
import kotlinx.coroutines.flow.Flow

interface PermissionRepository {
    fun getInstalledGeneralApps(): Flow<List<InstalledAppInfo>>
    suspend fun getAppPermissions(packageName: String): List<AppPermission>
    suspend fun togglePermission(packageName: String, permission: AppPermission, grant: Boolean): Result<Unit>
    suspend fun forceStopApp(packageName: String): Result<Unit>
    suspend fun revokeOverlayPermission(packageName: String): Result<Unit>
    suspend fun revokeAccessibilityPermission(packageName: String): Result<Unit>
    suspend fun uninstallApp(packageName: String): Result<Unit>
}
