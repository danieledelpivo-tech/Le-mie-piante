package com.lemiepiante.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lemiepiante.LeMiePianteApp
import com.lemiepiante.data.Plant
import com.lemiepiante.util.PhotoStorage
import kotlinx.coroutines.launch

data class PlantForm(
    val name: String = "",
    val species: String = "",
    val location: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val waterSummerDays: Int = 7,
    val waterWinterDays: Int = 14,
    val fertilizeSummerDays: Int = 30,
    val fertilizeWinterDays: Int = 0,
    /** Solo per piante nuove: quanti giorni fa è stata fatta l'ultima cura (null = non lo so). */
    val waterDaysAgo: Int? = null,
    val fertilizeDaysAgo: Int? = null,
    val nameError: Boolean = false,
)

class PlantEditViewModel(savedStateHandle: SavedStateHandle, private val app: LeMiePianteApp) : ViewModel() {
    private val repository = app.repository
    private val plantId: Long = savedStateHandle["plantId"] ?: -1L
    val isNew: Boolean = plantId <= 0

    var form by mutableStateOf(PlantForm())
        private set
    var locations by mutableStateOf(emptyList<String>())
        private set
    var loaded by mutableStateOf(isNew)
        private set

    private var original: Plant? = null
    private val createdPhotos = mutableListOf<String>()
    private var saved = false

    init {
        viewModelScope.launch {
            locations = repository.getLocations()
            if (!isNew) {
                repository.getPlant(plantId)?.let { plant ->
                    original = plant
                    form = PlantForm(
                        name = plant.name,
                        species = plant.species,
                        location = plant.location,
                        notes = plant.notes,
                        photoPath = plant.photoPath,
                        waterSummerDays = plant.waterSummerDays,
                        waterWinterDays = plant.waterWinterDays,
                        fertilizeSummerDays = plant.fertilizeSummerDays,
                        fertilizeWinterDays = plant.fertilizeWinterDays,
                    )
                }
                loaded = true
            }
        }
    }

    fun update(transform: (PlantForm) -> PlantForm) {
        form = transform(form)
    }

    fun onPhotoCreated(path: String) {
        createdPhotos += path
        form = form.copy(photoPath = path)
    }

    fun removePhoto() {
        form = form.copy(photoPath = null)
    }

    fun save(onSaved: () -> Unit) {
        val f = form
        if (f.name.isBlank()) {
            form = f.copy(nameError = true)
            return
        }
        viewModelScope.launch {
            val plant = (original ?: Plant(name = "")).copy(
                name = f.name.trim(),
                species = f.species.trim(),
                location = f.location.trim(),
                notes = f.notes.trim(),
                photoPath = f.photoPath,
                waterSummerDays = f.waterSummerDays,
                waterWinterDays = f.waterWinterDays,
                fertilizeSummerDays = f.fertilizeSummerDays,
                fertilizeWinterDays = f.fertilizeWinterDays,
            )
            repository.save(plant, f.waterDaysAgo, f.fertilizeDaysAgo)
            saved = true
            createdPhotos.filter { it != f.photoPath }.forEach(PhotoStorage::delete)
            original?.photoPath?.takeIf { it != f.photoPath }?.let(PhotoStorage::delete)
            onSaved()
        }
    }

    override fun onCleared() {
        if (!saved) createdPhotos.forEach(PhotoStorage::delete)
    }
}
