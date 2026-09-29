package com.admintool.deviceadmin.domain.usecase

import com.admintool.deviceadmin.data.repository.PermissionRepository
import javax.inject.Inject

class UninstallAppUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository
) {
    suspend operator fun invoke(packageName: String): Result<Unit> {
        return permissionRepository.uninstallApp(packageName)
    }
}
