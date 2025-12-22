package com.kipia.management.mobile.di

import android.content.Context
import com.kipia.management.mobile.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideDeviceDao(database: AppDatabase) = database.deviceDao()

    @Provides
    fun provideSchemeDao(database: AppDatabase) = database.schemeDao()

    @Provides
    fun provideDeviceLocationDao(database: AppDatabase) = database.deviceLocationDao()
}