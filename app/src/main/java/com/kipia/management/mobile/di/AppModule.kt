package com.kipia.management.mobile.di

import android.content.Context
import com.kipia.management.mobile.data.database.AppDatabase
import com.kipia.management.mobile.data.repository.DeviceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideDeviceRepository(database: AppDatabase): DeviceRepository {
        return DeviceRepository(database.deviceDao())
    }
}