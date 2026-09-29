package com.admintool.deviceadmin.domain.usecase

import com.admintool.deviceadmin.data.model.InstalledAppInfo
import com.admintool.deviceadmin.data.repository.PermissionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGeneralAppsUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository
) {
    operator fun invoke(): Flow<List<InstalledAppInfo>> {
        return permissionRepository.getInstalledGeneralApps()
    }
}
