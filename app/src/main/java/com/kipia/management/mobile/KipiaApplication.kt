package com.kipia.management.mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class KipiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Инициализация Timber для логирования
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("KipiaApplication created")
    }
}