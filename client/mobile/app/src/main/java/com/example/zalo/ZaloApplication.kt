package com.example.zalo

import android.app.Application
import com.example.zalo.util.CallManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ZaloApplication : Application() {
    
    @Inject
    lateinit var callManager: CallManager

    override fun onCreate() {
        super.onCreate()
        callManager.init(this)
    }
}
