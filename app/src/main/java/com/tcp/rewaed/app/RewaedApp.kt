package com.tcp.rewaed.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.tcp.rewaed.BuildConfig
import com.tcp.rewaed.R
import com.tcp.rewaed.service.WakeWordDetectionService
import com.tcp.rewaed.utils.SharedPref
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class RewaedApp : Application() {

    companion object {
        var instance: RewaedApp? = null
            private set
        var appContext: Context? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        instance = this
        appContext = applicationContext
        SharedPref.setStringPref(
            this,
            SharedPref.KEY_TOKEN_LENGTH,
            getString(R.string.token_length)
        )
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
    }

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            val serviceIntent = Intent(applicationContext, WakeWordDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            // Optionally stop the foreground service when the app is in the foreground
            val serviceIntent = Intent(applicationContext, WakeWordDetectionService::class.java)
            stopService(serviceIntent)
        }
    }
}