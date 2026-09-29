package com.admintool.deviceadmin.domain.usecase

import android.content.ComponentName
import com.admintool.deviceadmin.data.repository.DeviceAdminRepository
import javax.inject.Inject

class ForceDeactivateAdminUseCase @Inject constructor(
    private val repository: DeviceAdminRepository
) {
    suspend operator fun invoke(componentName: ComponentName, packageName: String): Result<Unit> {
        return repository.forceDeactivateAdmin(componentName, packageName)
    }
}
