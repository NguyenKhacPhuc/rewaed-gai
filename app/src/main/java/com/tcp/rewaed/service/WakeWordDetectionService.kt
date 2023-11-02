package com.tcp.rewaed.service

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tcp.rewaed.R
import com.tcp.rewaed.ui.activities.MainActivity

class WakeWordDetectionService : Service() {

    private lateinit var porcupineManager: PorcupineManager

    override fun onBind(intent: Intent?): IBinder? {
        // This example is not using a bound service, so return null.
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Call to startForeground goes here
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wake Word Detection Active")
            .setContentText("Listening for wake word...")
            .setSmallIcon(R.drawable.ic_voice)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)

        initPorcupine()

        return START_NOT_STICKY
    }

    private fun initPorcupine() {
        val keywordCallback = { _: Int ->
            // Code to execute when wake word is detected
            openMainActivity()
        }

        try {
            porcupineManager = PorcupineManager.Builder()
                .setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                .setAccessKey("pB6oSgDp+ATL1KXQ4v2jT9W9V6tpL00vnAnWdAHs74s4dpMeLspOfw==")
                .setSensitivity(0.7f)
                .build(applicationContext, keywordCallback)
            porcupineManager.start()
        } catch (e: PorcupineException) {
            e.printStackTrace()
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    override fun onDestroy() {
        porcupineManager.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
        const val ONGOING_NOTIFICATION_ID = 1
    }
}