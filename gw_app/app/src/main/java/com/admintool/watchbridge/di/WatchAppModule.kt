package com.admintool.watchbridge.di

import com.admintool.watchbridge.data.repository.HotspotRepositoryImpl
import com.admintool.watchbridge.domain.repository.HotspotRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WatchAppModule {

    @Binds
    @Singleton
    abstract fun bindHotspotRepository(
        impl: HotspotRepositoryImpl
    ): HotspotRepository
}
