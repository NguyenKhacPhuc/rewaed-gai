package com.tcp.rewaed.app

import android.app.Application
import android.content.Context
import com.tcp.rewaed.BuildConfig
import com.tcp.rewaed.R
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
    }
}