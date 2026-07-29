package com.kipia.management.mobile.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kipia.management.mobile.MainActivity
import com.kipia.management.mobile.R
import com.kipia.management.mobile.managers.SyncManager
import com.kipia.management.mobile.managers.SyncState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class SyncService : Service() {

    @Inject
    lateinit var syncManager: SyncManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observationJob: Job? = null
    
    companion object {
        const val CHANNEL_ID = "sync_event_channel_v15" 
        const val NOTIFICATION_ID = 1001
        const val RESULT_NOTIFICATION_ID = 1002
        
        const val ACTION_START_EXPORT = "ACTION_START_EXPORT"
        const val ACTION_START_IMPORT = "ACTION_START_IMPORT"
        const val EXTRA_URI = "EXTRA_URI"
        const val EXTRA_IMPORT_DELETED = "EXTRA_IMPORT_DELETED"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeSyncState()
    }

    private fun observeSyncState() {
        observationJob?.cancel()
        observationJob = serviceScope.launch {
            syncManager.syncState.collectLatest { state ->
                when (state) {
                    is SyncState.ExportSuccess -> {
                        showFinalNotification("Экспорт завершен", "Данные сохранены.")
                        stopServiceDelayed()
                    }
                    is SyncState.ImportSuccess -> {
                        showFinalNotification("Импорт завершен", "База данных обновлена.")
                        stopServiceDelayed()
                    }
                    is SyncState.Error -> {
                        showFinalNotification("Ошибка", state.message)
                        stopServiceDelayed()
                    }
                    is SyncState.ConflictsDetected,
                    is SyncState.GhostDevicesDetected -> {
                        showFinalNotification("Внимание", "Требуется действие пользователя.")
                        stopServiceDelayed()
                    }
                    is SyncState.Loading -> {
                        updateForegroundNotification(state.message)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun stopServiceDelayed() {
        serviceScope.launch {
            delay(1500)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_URI)
        }
        
        if (uri != null) {
            val notification = createForegroundNotification("Запуск...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            when (action) {
                ACTION_START_EXPORT -> syncManager.startExport(uri)
                ACTION_START_IMPORT -> {
                    val importDeleted = intent.getBooleanExtra(EXTRA_IMPORT_DELETED, false)
                    syncManager.startImport(uri, importDeleted)
                }
            }
        }
        return START_STICKY
    }

    private fun createForegroundNotification(message: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("КИПиА: Синхронизация")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_sync_notification) 
            .setLargeIcon(largeIcon)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            // Категория EVENT — самая "уважаемая" на Realme для отображения значка
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()
    }

    private fun updateForegroundNotification(message: String) {
        val notification = createForegroundNotification(message)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showFinalNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_sync_notification)
            .setLargeIcon(largeIcon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(RESULT_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "События синхронизации",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Важные уведомления о работе с данными"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
