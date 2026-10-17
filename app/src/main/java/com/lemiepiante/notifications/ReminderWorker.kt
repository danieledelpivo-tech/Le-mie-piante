package com.lemiepiante.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lemiepiante.LeMiePianteApp
import com.lemiepiante.domain.CareType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Controllo giornaliero: notifica le piante che oggi hanno bisogno di cure. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as LeMiePianteApp).repository
        val today = LocalDate.now()
        for (plant in repository.getPlants()) {
            for (type in CareType.entries) {
                val status = plant.status(type, today)
                if (status.isDue) {
                    ReminderNotifications.show(applicationContext, plant, status)
                } else {
                    ReminderNotifications.cancel(applicationContext, plant.id, type)
                }
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_care_check"
        private val REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(REMINDER_TIME)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val initialDelay = Duration.between(now, next).toMinutes()

            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
