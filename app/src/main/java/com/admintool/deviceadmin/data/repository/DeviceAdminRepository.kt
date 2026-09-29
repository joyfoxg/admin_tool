package com.admintool.deviceadmin.data.repository

import android.content.ComponentName
import com.admintool.deviceadmin.data.model.DeviceAdminApp
import com.admintool.deviceadmin.data.model.PrivilegeMode
import kotlinx.coroutines.flow.Flow

interface DeviceAdminRepository {
    fun getInstalledDeviceAdmins(): Flow<List<DeviceAdminApp>>
    suspend fun getPrivilegeMode(): PrivilegeMode
    suspend fun removeDeviceAdminStandard(componentName: ComponentName): Result<Unit>
    suspend fun forceDeactivateAdmin(componentName: ComponentName, packageName: String): Result<Unit>
    suspend fun activateAdmin(componentName: ComponentName): Result<Unit>
}
