package com.lemiepiante.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lemiepiante.LeMiePianteApp
import com.lemiepiante.domain.CareType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Gestisce i pulsanti "Fatto" e "Rimanda" direttamente dalla notifica. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val plantId = intent.getLongExtra(EXTRA_PLANT_ID, -1)
        val type = intent.getStringExtra(EXTRA_TYPE)?.let(CareType::valueOf) ?: return
        if (plantId < 0) return

        val repository = (context.applicationContext as LeMiePianteApp).repository
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_DONE -> {
                        // Per il concime riusa prodotto e dose dell'ultima volta.
                        val last = if (type == CareType.FERTILIZE) repository.lastFertilization(plantId) else null
                        repository.markDone(plantId, type, last?.product.orEmpty(), last?.dose.orEmpty())
                    }
                    ACTION_SNOOZE -> repository.snooze(plantId, type, ReminderNotifications.SNOOZE_DAYS_FROM_NOTIFICATION)
                }
                ReminderNotifications.cancel(context, plantId, type)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.lemiepiante.action.DONE"
        const val ACTION_SNOOZE = "com.lemiepiante.action.SNOOZE"
        const val EXTRA_PLANT_ID = "plant_id"
        const val EXTRA_TYPE = "care_type"
    }
}
