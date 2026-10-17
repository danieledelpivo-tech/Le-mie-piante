package com.lemiepiante

import android.app.Application
import com.lemiepiante.data.AppDatabase
import com.lemiepiante.data.PlantRepository
import com.lemiepiante.notifications.ReminderNotifications
import com.lemiepiante.notifications.ReminderWorker

class LeMiePianteApp : Application() {
    val repository: PlantRepository by lazy { PlantRepository(AppDatabase.get(this)) }

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.createChannel(this)
        ReminderWorker.schedule(this)
    }
}
