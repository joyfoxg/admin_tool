package com.admintool.deviceadmin.di

import com.admintool.deviceadmin.data.repository.DeviceAdminRepository
import com.admintool.deviceadmin.data.repository.DeviceAdminRepositoryImpl
import com.admintool.deviceadmin.data.repository.PermissionRepository
import com.admintool.deviceadmin.data.repository.PermissionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindDeviceAdminRepository(
        impl: DeviceAdminRepositoryImpl
    ): DeviceAdminRepository

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(
        impl: PermissionRepositoryImpl
    ): PermissionRepository
}
