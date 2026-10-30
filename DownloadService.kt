package com.example.steamgrab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Download game", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            job?.cancel()
            stopSelf()
            return START_NOT_STICKY
        }

        val appId = intent?.getIntExtra(EXTRA_APP_ID, -1) ?: -1
        val installDir = intent?.getStringExtra(EXTRA_INSTALL_DIR) ?: return START_NOT_STICKY

        startForegroundCompat(buildNotification("Menyiapkan download...", 0))
        acquireLocks()

        job = scope.launch {
            // TODO: panggil SteamRepository.downloadGame(appId, installDir, ...) di sini.
            // Setiap progres: updateNotification(percent, text)
            // Saat selesai atau gagal: stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        job?.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        wifiLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    fun updateNotification(percent: Int, text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification(text, percent))
    }

    private fun acquireLocks() {
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SteamGrab:download")
            .apply { acquire() }
        wifiLock = getSystemService(WifiManager::class.java)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "SteamGrab:wifi")
            .apply { acquire() }
    }

    private fun startForegroundCompat(n: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun buildNotification(text: String, percent: Int): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, DownloadService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("SteamGrab sedang mengunduh")
            .setContentText(text)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .addAction(0, "Batal", stopIntent)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.example.steamgrab.STOP"
        const val EXTRA_APP_ID = "app_id"
        const val EXTRA_INSTALL_DIR = "install_dir"
        private const val CHANNEL_ID = "download"
        private const val NOTIF_ID = 1
    }
}
