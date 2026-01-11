package com.kipia.management.mobile.di

import android.content.Context
import androidx.room.Room
import com.kipia.management.mobile.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        Timber.d("DATABASE: Создаем AppDatabase")
        return try {
            val db = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "kipia_management.db"
            )
                .fallbackToDestructiveMigration()
                .build()
            Timber.d("DATABASE: AppDatabase создана успешно")
            db
        } catch (e: Exception) {
            Timber.e("DATABASE: Ошибка создания AppDatabase: ${e.message}")
            throw e
        }
    }

    @Provides
    fun provideDeviceDao(database: AppDatabase) = database.deviceDao()

    @Provides
    fun provideSchemeDao(database: AppDatabase) = database.schemeDao()

    @Provides
    fun provideDeviceLocationDao(database: AppDatabase) = database.deviceLocationDao()
}