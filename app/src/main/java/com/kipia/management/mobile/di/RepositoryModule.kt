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

/**
 * Абстрактный класс для модуля репозиториев.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Метод для предоставления экземпляра репозитория схем.
     */
    @Binds
    @Singleton
    abstract fun bindSchemeRepository(
        schemeRepositoryImpl: SchemeRepositoryImpl
    ): SchemeRepository

    /**
     * Метод для предоставления экземпляра репозитория локаций устройств.
     */
    @Binds
    @Singleton
    abstract fun bindDeviceLocationRepository(
        deviceLocationRepositoryImpl: DeviceLocationRepositoryImpl
    ): DeviceLocationRepository
}