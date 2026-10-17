package com.lemiepiante.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lemiepiante.LeMiePianteApp
import com.lemiepiante.data.Plant
import com.lemiepiante.domain.CareSchedule
import com.lemiepiante.domain.CareStatus
import com.lemiepiante.domain.CareType
import com.lemiepiante.domain.Season
import com.lemiepiante.domain.Seasons
import com.lemiepiante.notifications.ReminderNotifications
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlantListItem(val plant: Plant, val water: CareStatus, val fertilize: CareStatus) {
    val needsCare: Boolean get() = water.isDue || fertilize.isDue
}

data class PlantListUiState(
    val items: List<PlantListItem> = emptyList(),
    val locations: List<String> = emptyList(),
    val selectedLocation: String? = null,
    val season: Season = Seasons.of(LocalDate.now()),
    val totalPlants: Int = 0,
    val loading: Boolean = true,
)

/** Messaggio per la snackbar con possibilità di annullare l'azione. */
data class UndoMessage(val text: String, val eventId: Long)

class PlantListViewModel(private val app: LeMiePianteApp) : ViewModel() {
    private val repository = app.repository
    private val today = MutableStateFlow(LocalDate.now())
    private val selectedLocation = MutableStateFlow<String?>(null)

    private val _messages = MutableSharedFlow<UndoMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<UndoMessage> = _messages.asSharedFlow()

    val uiState: StateFlow<PlantListUiState> =
        combine(repository.observePlants(), today, selectedLocation) { plants, day, location ->
            val locations = plants.map { it.location }.filter { it.isNotBlank() }.distinct().sortedBy { it.lowercase() }
            val selected = location?.takeIf { it in locations }
            val items = plants
                .filter { selected == null || it.location == selected }
                .map { PlantListItem(it, it.status(CareType.WATER, day), it.status(CareType.FERTILIZE, day)) }
                .sortedWith(
                    compareBy<PlantListItem> { CareSchedule.sortKey(listOf(it.water, it.fertilize)) }
                        .thenBy { it.plant.name.lowercase() },
                )
            PlantListUiState(items, locations, selected, Seasons.of(day), plants.size, loading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlantListUiState())

    fun refreshDate() {
        today.value = LocalDate.now()
    }

    fun selectLocation(location: String?) {
        selectedLocation.value = location
    }

    fun markDone(plant: Plant, type: CareType) {
        viewModelScope.launch {
            val last = if (type == CareType.FERTILIZE) repository.lastFertilization(plant.id) else null
            val eventId = repository.markDone(plant.id, type, last?.product.orEmpty(), last?.dose.orEmpty()) ?: return@launch
            ReminderNotifications.cancel(app, plant.id, type)
            val verb = if (type == CareType.WATER) "innaffiata" else "concimata"
            _messages.emit(UndoMessage("${plant.name} $verb", eventId))
        }
    }

    fun undo(eventId: Long) {
        viewModelScope.launch { repository.deleteEvent(eventId) }
    }
}
