package com.lemiepiante.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lemiepiante.domain.CareType

enum class EventKind(val type: CareType, val isSnooze: Boolean) {
    WATERED(CareType.WATER, false),
    FERTILIZED(CareType.FERTILIZE, false),
    WATER_SNOOZED(CareType.WATER, true),
    FERTILIZE_SNOOZED(CareType.FERTILIZE, true);

    companion object {
        fun done(type: CareType) = if (type == CareType.WATER) WATERED else FERTILIZED
        fun snoozed(type: CareType) = if (type == CareType.WATER) WATER_SNOOZED else FERTILIZE_SNOOZED
    }
}

@Entity(
    tableName = "care_events",
    foreignKeys = [
        ForeignKey(
            entity = Plant::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plantId")],
)
data class CareEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val kind: EventKind,
    val epochDay: Long,
    val timestampMillis: Long = System.currentTimeMillis(),
    /** Solo per la concimazione. */
    val product: String = "",
    val dose: String = "",
    /** Solo per i rinvii ("Ancora umida"). */
    val snoozeDays: Int = 0,
)
