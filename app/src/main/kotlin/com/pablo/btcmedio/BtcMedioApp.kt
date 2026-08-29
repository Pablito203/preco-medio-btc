package com.pablo.btcmedio

import android.app.Application

class BtcMedioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
