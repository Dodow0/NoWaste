package com.nowaste.app

import android.app.Application
import com.nowaste.app.notifications.ReminderScheduler

class NoWasteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.scheduleDaily(this, ServiceLocator.appSettings(this))
    }
}
