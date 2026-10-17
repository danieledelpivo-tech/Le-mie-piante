package com.lemiepiante.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lemiepiante.ui.AppViewModelFactory
import com.lemiepiante.ui.components.DaysStepper
import com.lemiepiante.ui.components.PlantPhoto
import com.lemiepiante.util.PhotoStorage

/** Opzioni per "ultima volta" quando si aggiunge una pianta: null = non lo so. */
private val LAST_TIME_OPTIONS = listOf<Pair<Int?, String>>(
    null to "Non so",
    0 to "Oggi",
    1 to "Ieri",
    3 to "3 giorni fa",
    7 to "1 settimana fa",
    14 to "2 settimane fa",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlantEditScreen(
    onDone: () -> Unit,
    viewModel: PlantEditViewModel = viewModel(factory = AppViewModelFactory),
) {
    val context = LocalContext.current
    val form = viewModel.form
    var pendingCameraFile by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val path = pendingCameraFile ?: return@rememberLauncherForActivityResult
        if (success) viewModel.onPhotoCreated(path) else PhotoStorage.delete(path)
        pendingCameraFile = null
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { PhotoStorage.copyFromUri(context, it) }?.let { viewModel.onPhotoCreated(it.absolutePath) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Nuova pianta" else "Modifica pianta") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro") }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onSaved = onDone) }, enabled = viewModel.loaded) { Text("Salva") }
                },
            )
        },
    ) { padding ->
        if (!viewModel.loaded) return@Scaffold
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Foto
            PlantPhoto(form.photoPath, Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(20.dp)))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val file = PhotoStorage.newPhotoFile(context)
                        pendingCameraFile = file.absolutePath
                        takePicture.launch(PhotoStorage.uriFor(context, file))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scatta")
                }
                OutlinedButton(
                    onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Galleria")
                }
            }
            if (form.photoPath != null) {
                TextButton(onClick = viewModel::removePhoto) { Text("Rimuovi foto") }
            }

            // Dati principali
            OutlinedTextField(
                value = form.name,
                onValueChange = { v -> viewModel.update { it.copy(name = v, nameError = false) } },
                label = { Text("Nome *") },
                placeholder = { Text("es. Monstera del salotto") },
                isError = form.nameError,
                supportingText = if (form.nameError) { { Text("Inserisci un nome") } } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.species,
                onValueChange = { v -> viewModel.update { it.copy(species = v) } },
                label = { Text("Specie (facoltativa)") },
                placeholder = { Text("es. Monstera deliciosa") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Column {
                OutlinedTextField(
                    value = form.location,
                    onValueChange = { v -> viewModel.update { it.copy(location = v) } },
                    label = { Text("Posizione (facoltativa)") },
                    placeholder = { Text("es. Salotto") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                val suggestions = viewModel.locations.filter { it != form.location }
                if (suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { location ->
                            SuggestionChip(
                                onClick = { viewModel.update { it.copy(location = location) } },
                                label = { Text(location) },
                            )
                        }
                    }
                }
            }

            // Frequenze
            SectionCard(
                title = "💧 Acqua",
                hint = "Estate = da marzo a ottobre, inverno = da novembre a febbraio.",
            ) {
                DaysStepper("Estate", form.waterSummerDays, { v -> viewModel.update { it.copy(waterSummerDays = v) } })
                DaysStepper("Inverno", form.waterWinterDays, { v -> viewModel.update { it.copy(waterWinterDays = v) } })
                if (viewModel.isNew) {
                    LastTimeSelector("Ultima innaffiatura", form.waterDaysAgo) { v -> viewModel.update { it.copy(waterDaysAgo = v) } }
                }
            }
            SectionCard(
                title = "🌱 Concime",
                hint = "Molte piante d'appartamento in inverno non vanno concimate: lascia \"Sospeso\".",
            ) {
                DaysStepper("Estate", form.fertilizeSummerDays, { v -> viewModel.update { it.copy(fertilizeSummerDays = v) } })
                DaysStepper("Inverno", form.fertilizeWinterDays, { v -> viewModel.update { it.copy(fertilizeWinterDays = v) } })
                if (viewModel.isNew) {
                    LastTimeSelector("Ultima concimazione", form.fertilizeDaysAgo) { v -> viewModel.update { it.copy(fertilizeDaysAgo = v) } }
                }
            }

            // Note
            OutlinedTextField(
                value = form.notes,
                onValueChange = { v -> viewModel.update { it.copy(notes = v) } },
                label = { Text("Note") },
                placeholder = { Text("es. solo acqua a temperatura ambiente, non spostare dalla finestra") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, hint: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LastTimeSelector(label: String, selected: Int?, onSelect: (Int?) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LAST_TIME_OPTIONS.forEach { (days, text) ->
                FilterChip(selected = selected == days, onClick = { onSelect(days) }, label = { Text(text) })
            }
        }
    }
}

