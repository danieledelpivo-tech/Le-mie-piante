package com.lemiepiante.domain

import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

enum class CareType { WATER, FERTILIZE }

enum class Season { SUMMER, WINTER }

object Seasons {
    /** Mesi di riposo vegetativo: in questi mesi si usano le frequenze "invernali". */
    val WINTER_MONTHS = setOf(Month.NOVEMBER, Month.DECEMBER, Month.JANUARY, Month.FEBRUARY)

    fun of(date: LocalDate): Season =
        if (date.month in WINTER_MONTHS) Season.WINTER else Season.SUMMER
}

enum class Urgency {
    /** Frequenza impostata a 0: cura sospesa (es. niente concime d'inverno). */
    DISABLED,
    /** Mai registrata: non sappiamo quando è stata fatta l'ultima volta. */
    NEVER_DONE,
    OVERDUE,
    TODAY,
    SOON,
    OK,
}

data class CareStatus(
    val type: CareType,
    val season: Season,
    /** Frequenza in giorni per la stagione corrente; 0 = sospesa. */
    val intervalDays: Int,
    val lastDone: LocalDate?,
    val dueDate: LocalDate?,
    /** Giorni alla scadenza: negativo = in ritardo. */
    val daysLeft: Long?,
    /** True se la scadenza è stata rimandata con "Ancora umida". */
    val snoozed: Boolean,
    val urgency: Urgency,
) {
    val isDue: Boolean
        get() = urgency == Urgency.OVERDUE || urgency == Urgency.TODAY || urgency == Urgency.NEVER_DONE
}

object CareSchedule {
    /** Entro quanti giorni una scadenza è considerata "a breve". */
    const val SOON_THRESHOLD_DAYS = 2

    fun status(
        type: CareType,
        summerIntervalDays: Int,
        winterIntervalDays: Int,
        lastDone: LocalDate?,
        snoozeUntil: LocalDate?,
        today: LocalDate,
    ): CareStatus {
        val season = Seasons.of(today)
        val interval = if (season == Season.WINTER) winterIntervalDays else summerIntervalDays
        if (interval <= 0) {
            return CareStatus(type, season, 0, lastDone, null, null, snoozed = false, urgency = Urgency.DISABLED)
        }

        val baseDue = lastDone?.plusDays(interval.toLong()) ?: today
        val snoozed = snoozeUntil != null && snoozeUntil.isAfter(baseDue)
        val due = if (snoozed) snoozeUntil!! else baseDue
        val daysLeft = ChronoUnit.DAYS.between(today, due)

        val urgency = when {
            lastDone == null && daysLeft <= 0 -> Urgency.NEVER_DONE
            daysLeft < 0 -> Urgency.OVERDUE
            daysLeft == 0L -> Urgency.TODAY
            daysLeft <= SOON_THRESHOLD_DAYS -> Urgency.SOON
            else -> Urgency.OK
        }
        return CareStatus(type, season, interval, lastDone, due, daysLeft, snoozed, urgency)
    }

    /**
     * Chiave di ordinamento per la lista: prima le piante con la scadenza più vicina
     * (o già passata), in fondo quelle con tutte le cure sospese.
     */
    fun sortKey(statuses: List<CareStatus>): Long =
        statuses.filter { it.urgency != Urgency.DISABLED }
            .minOfOrNull { if (it.urgency == Urgency.NEVER_DONE) Long.MIN_VALUE else it.daysLeft!! }
            ?: Long.MAX_VALUE
}
