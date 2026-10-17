package com.lemiepiante.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lemiepiante.MainActivity
import com.lemiepiante.R
import com.lemiepiante.data.Plant
import com.lemiepiante.domain.CareStatus
import com.lemiepiante.domain.CareType
import com.lemiepiante.domain.Urgency
import com.lemiepiante.domain.giorni

object ReminderNotifications {
    private const val CHANNEL_ID = "care_reminders"
    const val SNOOZE_DAYS_FROM_NOTIFICATION = 2

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Promemoria cure", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Avvisi quando una pianta ha bisogno di acqua o concime"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notificationId(plantId: Long, type: CareType): Int = (plantId * 2 + type.ordinal).toInt()

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // verificato da canNotify()
    fun show(context: Context, plant: Plant, status: CareStatus) {
        if (!canNotify(context)) return
        val id = notificationId(plant.id, status.type)

        val title = when (status.type) {
            CareType.WATER -> "💧 ${plant.name} ha sete?"
            CareType.FERTILIZE -> "🌱 ${plant.name}: è ora del concime"
        }
        val text = when (status.urgency) {
            Urgency.NEVER_DONE -> "Registra l'ultima volta per far partire il timer"
            Urgency.OVERDUE -> "In ritardo di ${giorni(-(status.daysLeft ?: 0))}"
            else -> "In programma per oggi"
        } + if (status.type == CareType.WATER) ". Controlla prima il terreno con un dito." else "."

        val openApp = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_PLANT_ID, plant.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(0, "Fatto", actionIntent(context, ReminderActionReceiver.ACTION_DONE, plant.id, status.type, id * 10 + 1))
            .addAction(
                0,
                if (status.type == CareType.WATER) "Ancora umida (+$SNOOZE_DAYS_FROM_NOTIFICATION gg)" else "Rimanda (+$SNOOZE_DAYS_FROM_NOTIFICATION gg)",
                actionIntent(context, ReminderActionReceiver.ACTION_SNOOZE, plant.id, status.type, id * 10 + 2),
            )
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun cancel(context: Context, plantId: Long, type: CareType) {
        NotificationManagerCompat.from(context).cancel(notificationId(plantId, type))
    }

    fun cancelAll(context: Context, plantId: Long) {
        CareType.entries.forEach { cancel(context, plantId, it) }
    }

    private fun actionIntent(context: Context, action: String, plantId: Long, type: CareType, requestCode: Int) =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, ReminderActionReceiver::class.java)
                .setAction(action)
                .putExtra(ReminderActionReceiver.EXTRA_PLANT_ID, plantId)
                .putExtra(ReminderActionReceiver.EXTRA_TYPE, type.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
