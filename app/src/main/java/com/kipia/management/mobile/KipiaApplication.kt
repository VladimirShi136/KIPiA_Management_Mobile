package com.kipia.management.mobile

import android.app.Application
import com.kipia.management.mobile.data.DatabaseInitializer
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class KipiaApplication : Application() {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    override fun onCreate() {
        super.onCreate()

        // Инициализация Timber для логирования
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("KipiaApplication created")

        // Инициализируем базу тестовыми данными
        databaseInitializer.initialize()
    }
}