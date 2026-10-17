package com.lemiepiante.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lemiepiante.domain.CareSchedule
import com.lemiepiante.domain.CareStatus
import com.lemiepiante.domain.CareType
import java.time.LocalDate

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val species: String = "",
    val location: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    /** Frequenze in giorni; 0 = sospesa in quella stagione. */
    val waterSummerDays: Int = 7,
    val waterWinterDays: Int = 14,
    val fertilizeSummerDays: Int = 30,
    val fertilizeWinterDays: Int = 0,
    /** Date salvate come LocalDate.toEpochDay(). */
    val lastWateredEpochDay: Long? = null,
    val lastFertilizedEpochDay: Long? = null,
    val waterSnoozeUntilEpochDay: Long? = null,
    val fertilizeSnoozeUntilEpochDay: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    fun status(type: CareType, today: LocalDate = LocalDate.now()): CareStatus = when (type) {
        CareType.WATER -> CareSchedule.status(
            type, waterSummerDays, waterWinterDays,
            lastWateredEpochDay?.let(LocalDate::ofEpochDay),
            waterSnoozeUntilEpochDay?.let(LocalDate::ofEpochDay),
            today,
        )
        CareType.FERTILIZE -> CareSchedule.status(
            type, fertilizeSummerDays, fertilizeWinterDays,
            lastFertilizedEpochDay?.let(LocalDate::ofEpochDay),
            fertilizeSnoozeUntilEpochDay?.let(LocalDate::ofEpochDay),
            today,
        )
    }
}
