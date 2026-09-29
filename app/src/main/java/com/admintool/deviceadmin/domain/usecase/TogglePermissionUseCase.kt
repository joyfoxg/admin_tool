package com.admintool.deviceadmin.domain.usecase

import com.admintool.deviceadmin.data.model.AppPermission
import com.admintool.deviceadmin.data.repository.PermissionRepository
import javax.inject.Inject

class TogglePermissionUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository
) {
    suspend operator fun invoke(packageName: String, permission: AppPermission, grant: Boolean): Result<Unit> {
        return permissionRepository.togglePermission(packageName, permission, grant)
    }
}
