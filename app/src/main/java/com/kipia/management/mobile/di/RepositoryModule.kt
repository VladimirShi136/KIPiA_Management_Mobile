package com.kipia.management.mobile.di

import com.kipia.management.mobile.repository.*
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
    abstract fun bindDeviceRepository(
        repositoryImpl: DeviceRepositoryImpl
    ): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindSchemeRepository(
        repositoryImpl: SchemeRepositoryImpl
    ): SchemeRepository

    @Binds
    @Singleton
    abstract fun bindDeviceLocationRepository(
        repositoryImpl: DeviceLocationRepositoryImpl
    ): DeviceLocationRepository
}