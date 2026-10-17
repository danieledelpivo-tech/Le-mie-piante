package com.lemiepiante.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY name COLLATE NOCASE")
    fun observePlants(): Flow<List<Plant>>

    @Query("SELECT * FROM plants ORDER BY name COLLATE NOCASE")
    suspend fun getPlants(): List<Plant>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun observePlant(id: Long): Flow<Plant?>

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun getPlant(id: Long): Plant?

    @Query("SELECT DISTINCT location FROM plants WHERE location != '' ORDER BY location COLLATE NOCASE")
    suspend fun getLocations(): List<String>

    @Insert
    suspend fun insert(plant: Plant): Long

    @Update
    suspend fun update(plant: Plant)

    @Delete
    suspend fun delete(plant: Plant)

    @Query("SELECT * FROM care_events WHERE plantId = :plantId ORDER BY epochDay DESC, timestampMillis DESC")
    fun observeEvents(plantId: Long): Flow<List<CareEvent>>

    @Query("SELECT * FROM care_events WHERE id = :id")
    suspend fun getEvent(id: Long): CareEvent?

    @Query("SELECT MAX(epochDay) FROM care_events WHERE plantId = :plantId AND kind = :kind")
    suspend fun lastEventDay(plantId: Long, kind: String): Long?

    @Query("SELECT * FROM care_events WHERE plantId = :plantId AND kind = 'FERTILIZED' ORDER BY epochDay DESC, timestampMillis DESC LIMIT 1")
    suspend fun lastFertilization(plantId: Long): CareEvent?

    @Insert
    suspend fun insertEvent(event: CareEvent): Long

    @Delete
    suspend fun deleteEvent(event: CareEvent)
}
