package com.lemiepiante.data

import androidx.room.withTransaction
import com.lemiepiante.domain.CareType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PlantRepository(private val db: AppDatabase) {
    private val dao = db.plantDao()

    fun observePlants(): Flow<List<Plant>> = dao.observePlants()
    fun observePlant(id: Long): Flow<Plant?> = dao.observePlant(id)
    fun observeEvents(plantId: Long): Flow<List<CareEvent>> = dao.observeEvents(plantId)

    suspend fun getPlants(): List<Plant> = dao.getPlants()
    suspend fun getPlant(id: Long): Plant? = dao.getPlant(id)
    suspend fun getLocations(): List<String> = dao.getLocations()
    suspend fun lastFertilization(plantId: Long): CareEvent? = dao.lastFertilization(plantId)

    /**
     * Salva una pianta. Per una pianta nuova si può indicare quanti giorni fa
     * è stata innaffiata/concimata l'ultima volta (null = non lo so).
     */
    suspend fun save(plant: Plant, waterDaysAgo: Int? = null, fertilizeDaysAgo: Int? = null): Long =
        db.withTransaction {
            if (plant.id != 0L) {
                dao.update(plant)
                return@withTransaction plant.id
            }
            val today = LocalDate.now()
            val waterDay = waterDaysAgo?.let { today.minusDays(it.toLong()).toEpochDay() }
            val fertDay = fertilizeDaysAgo?.let { today.minusDays(it.toLong()).toEpochDay() }
            val id = dao.insert(plant.copy(lastWateredEpochDay = waterDay, lastFertilizedEpochDay = fertDay))
            waterDay?.let { dao.insertEvent(CareEvent(plantId = id, kind = EventKind.WATERED, epochDay = it)) }
            fertDay?.let { dao.insertEvent(CareEvent(plantId = id, kind = EventKind.FERTILIZED, epochDay = it)) }
            id
        }

    suspend fun delete(plant: Plant) = dao.delete(plant)

    /** Registra un'innaffiatura o concimazione di oggi. Ritorna l'id dell'evento (per "Annulla"). */
    suspend fun markDone(
        plantId: Long,
        type: CareType,
        product: String = "",
        dose: String = "",
        today: LocalDate = LocalDate.now(),
    ): Long? = db.withTransaction {
        val plant = dao.getPlant(plantId) ?: return@withTransaction null
        val day = today.toEpochDay()
        val eventId = dao.insertEvent(
            CareEvent(plantId = plantId, kind = EventKind.done(type), epochDay = day, product = product.trim(), dose = dose.trim()),
        )
        dao.update(
            when (type) {
                CareType.WATER -> plant.copy(lastWateredEpochDay = day, waterSnoozeUntilEpochDay = null)
                CareType.FERTILIZE -> plant.copy(lastFertilizedEpochDay = day, fertilizeSnoozeUntilEpochDay = null)
            },
        )
        eventId
    }

    /** "Ancora umida": sposta il promemoria di [days] giorni senza azzerare il timer. */
    suspend fun snooze(plantId: Long, type: CareType, days: Int, today: LocalDate = LocalDate.now()): Long? =
        db.withTransaction {
            val plant = dao.getPlant(plantId) ?: return@withTransaction null
            val until = today.plusDays(days.toLong()).toEpochDay()
            val eventId = dao.insertEvent(
                CareEvent(plantId = plantId, kind = EventKind.snoozed(type), epochDay = today.toEpochDay(), snoozeDays = days),
            )
            dao.update(
                when (type) {
                    CareType.WATER -> plant.copy(waterSnoozeUntilEpochDay = until)
                    CareType.FERTILIZE -> plant.copy(fertilizeSnoozeUntilEpochDay = until)
                },
            )
            eventId
        }

    suspend fun deleteEvent(eventId: Long) {
        dao.getEvent(eventId)?.let { deleteEvent(it) }
    }

    /** Elimina un evento dallo storico e ricalcola le date della pianta. */
    suspend fun deleteEvent(event: CareEvent) = db.withTransaction {
        dao.deleteEvent(event)
        val plant = dao.getPlant(event.plantId) ?: return@withTransaction
        val updated = if (event.kind.isSnooze) {
            val target = event.epochDay + event.snoozeDays
            when (event.kind.type) {
                CareType.WATER -> if (plant.waterSnoozeUntilEpochDay == target) plant.copy(waterSnoozeUntilEpochDay = null) else plant
                CareType.FERTILIZE -> if (plant.fertilizeSnoozeUntilEpochDay == target) plant.copy(fertilizeSnoozeUntilEpochDay = null) else plant
            }
        } else {
            val last = dao.lastEventDay(event.plantId, event.kind.name)
            when (event.kind.type) {
                CareType.WATER -> plant.copy(lastWateredEpochDay = last)
                CareType.FERTILIZE -> plant.copy(lastFertilizedEpochDay = last)
            }
        }
        if (updated != plant) dao.update(updated)
    }
}
