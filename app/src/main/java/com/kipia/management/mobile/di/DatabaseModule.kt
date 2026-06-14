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


@Module // Аннотация модуля
@InstallIn(SingletonComponent::class) // Аннотация установки модуля в SingletonComponent
/**
 * Объект модуля базы данных.
 */
object DatabaseModule {

    /**
     * Метод для предоставления экземпляра базы данных.
     */
    @Provides // Аннотация провайдера. Это означает, что этот метод будет создан для предоставления зависимостей.
    @Singleton // Аннотация синглтона. Это означает, что только один экземпляр этого объекта будет создан.
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

    /**
     * Метод для предоставления экземпляра DAO устройств.
     */
    @Provides
    fun provideDeviceDao(database: AppDatabase) = database.deviceDao()

    /**
     * Метод для предоставления экземпляра DAO схем.
     */
    @Provides
    fun provideSchemeDao(database: AppDatabase) = database.schemeDao()

    /**
     * Метод для предоставления экземпляра DAO локаций устройств.
     */
    @Provides
    fun provideDeviceLocationDao(database: AppDatabase) = database.deviceLocationDao()
}