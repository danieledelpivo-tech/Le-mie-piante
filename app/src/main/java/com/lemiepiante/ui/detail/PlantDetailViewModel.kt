package com.lemiepiante.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lemiepiante.LeMiePianteApp
import com.lemiepiante.data.CareEvent
import com.lemiepiante.data.EventKind
import com.lemiepiante.data.Plant
import com.lemiepiante.domain.CareStatus
import com.lemiepiante.domain.CareType
import com.lemiepiante.notifications.ReminderNotifications
import com.lemiepiante.util.PhotoStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlantDetailUiState(
    val plant: Plant? = null,
    val water: CareStatus? = null,
    val fertilize: CareStatus? = null,
    val events: List<CareEvent> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val loading: Boolean = true,
) {
    val lastFertilization: CareEvent? get() = events.firstOrNull { it.kind == EventKind.FERTILIZED }
}

class PlantDetailViewModel(savedStateHandle: SavedStateHandle, private val app: LeMiePianteApp) : ViewModel() {
    private val repository = app.repository
    private val plantId: Long = checkNotNull(savedStateHandle["plantId"])
    private val today = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<PlantDetailUiState> =
        combine(repository.observePlant(plantId), repository.observeEvents(plantId), today) { plant, events, day ->
            PlantDetailUiState(
                plant = plant,
                water = plant?.status(CareType.WATER, day),
                fertilize = plant?.status(CareType.FERTILIZE, day),
                events = events,
                today = day,
                loading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlantDetailUiState())

    fun refreshDate() {
        today.value = LocalDate.now()
    }

    fun markDone(type: CareType, product: String = "", dose: String = "") {
        viewModelScope.launch {
            repository.markDone(plantId, type, product, dose)
            ReminderNotifications.cancel(app, plantId, type)
        }
    }

    fun snooze(type: CareType, days: Int) {
        viewModelScope.launch {
            repository.snooze(plantId, type, days)
            ReminderNotifications.cancel(app, plantId, type)
        }
    }

    fun deleteEvent(event: CareEvent) {
        viewModelScope.launch { repository.deleteEvent(event) }
    }

    fun deletePlant(onDeleted: () -> Unit) {
        val plant = uiState.value.plant ?: return
        viewModelScope.launch {
            repository.delete(plant)
            PhotoStorage.delete(plant.photoPath)
            ReminderNotifications.cancelAll(app, plant.id)
            onDeleted()
        }
    }
}
