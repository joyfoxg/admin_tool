package com.admintool.deviceadmin.domain.usecase

import com.admintool.deviceadmin.data.model.PrivilegeMode
import com.admintool.deviceadmin.data.repository.DeviceAdminRepository
import javax.inject.Inject

class CheckPrivilegeModeUseCase @Inject constructor(
    private val repository: DeviceAdminRepository
) {
    suspend operator fun invoke(): PrivilegeMode {
        return repository.getPrivilegeMode()
    }
}
