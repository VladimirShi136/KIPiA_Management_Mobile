package com.kipia.management.mobile.di

import com.kipia.management.mobile.repository.DeviceLocationRepository
import com.kipia.management.mobile.repository.DeviceLocationRepositoryImpl
import com.kipia.management.mobile.repository.SchemeRepository
import com.kipia.management.mobile.repository.SchemeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSchemeRepository(
        schemeRepositoryImpl: SchemeRepositoryImpl
    ): SchemeRepository

    @Binds
    @Singleton
    abstract fun bindDeviceLocationRepository(
        deviceLocationRepositoryImpl: DeviceLocationRepositoryImpl
    ): DeviceLocationRepository
}