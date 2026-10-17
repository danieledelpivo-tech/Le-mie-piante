package com.lemiepiante.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val ITALIAN = Locale.ITALIAN
private val shortDate = DateTimeFormatter.ofPattern("d MMM", ITALIAN)
private val longDate = DateTimeFormatter.ofPattern("EEE d MMM yyyy", ITALIAN)

fun giorni(n: Long): String = if (n == 1L) "1 giorno" else "$n giorni"

fun LocalDate.shortText(): String = format(shortDate)

fun LocalDate.longText(): String = format(longDate).replaceFirstChar { it.titlecase(ITALIAN) }

fun LocalDate.agoText(today: LocalDate): String = when (val d = ChronoUnit.DAYS.between(this, today)) {
    0L -> "oggi"
    1L -> "ieri"
    else -> "${giorni(d)} fa"
}

/** Testo principale della card: "Tra 3 giorni", "Oggi", "In ritardo di 2 giorni"... */
fun CareStatus.headline(): String {
    val d = daysLeft ?: 0
    return when (urgency) {
        Urgency.DISABLED -> "Sospesa"
        Urgency.NEVER_DONE -> "Da registrare"
        Urgency.OVERDUE -> "In ritardo di ${giorni(-d)}"
        Urgency.TODAY -> "Oggi"
        Urgency.SOON, Urgency.OK -> if (d == 1L) "Domani" else "Tra ${giorni(d)}"
    }
}

/** Testo breve per i chip della lista. */
fun CareStatus.chipText(): String {
    val d = daysLeft ?: 0
    return when (urgency) {
        Urgency.DISABLED -> "Sospeso"
        Urgency.NEVER_DONE -> "Da registrare"
        Urgency.OVERDUE -> "Ritardo ${-d} gg"
        Urgency.TODAY -> "Oggi"
        Urgency.SOON, Urgency.OK -> if (d == 1L) "Domani" else "Tra $d gg"
    }
}

fun Season.label(): String = when (this) {
    Season.SUMMER -> "estiva"
    Season.WINTER -> "invernale"
}

fun CareType.label(): String = when (this) {
    CareType.WATER -> "Acqua"
    CareType.FERTILIZE -> "Concime"
}
