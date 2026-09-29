package com.admintool.deviceadmin.domain.usecase

import com.admintool.deviceadmin.data.model.DeviceAdminApp
import com.admintool.deviceadmin.data.repository.DeviceAdminRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDeviceAdminsUseCase @Inject constructor(
    private val repository: DeviceAdminRepository
) {
    operator fun invoke(): Flow<List<DeviceAdminApp>> {
        return repository.getInstalledDeviceAdmins()
    }
}
