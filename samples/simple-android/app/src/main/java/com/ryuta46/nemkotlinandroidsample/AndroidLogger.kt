package com.ryuta46.nemkotlinandroidsample

import android.util.Log
import com.ryuta46.nemkotlin.util.Logger

class AndroidLogger : Logger{
    companion object {
        private const val TAG = "NemKotlinAndroidSample"
    }

    override fun log(level: Logger.Level, message: String) {
        when (level){
            Logger.Level.Verbose -> Log.v(TAG, message)
            Logger.Level.Debug -> Log.d(TAG, message)
            Logger.Level.Info -> Log.i(TAG, message)
            Logger.Level.Warn -> Log.w(TAG, message)
            Logger.Level.Error -> Log.e(TAG, message)
            else -> { /* Do nothing */ }
        }
    }
}