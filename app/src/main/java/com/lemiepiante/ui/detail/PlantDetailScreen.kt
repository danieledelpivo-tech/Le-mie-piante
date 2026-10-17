package com.lemiepiante.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lemiepiante.data.CareEvent
import com.lemiepiante.data.EventKind
import com.lemiepiante.domain.CareStatus
import com.lemiepiante.domain.CareType
import com.lemiepiante.domain.Urgency
import com.lemiepiante.domain.agoText
import com.lemiepiante.domain.headline
import com.lemiepiante.domain.label
import com.lemiepiante.domain.longText
import com.lemiepiante.domain.shortText
import com.lemiepiante.ui.AppViewModelFactory
import com.lemiepiante.ui.components.FertilizeDialog
import com.lemiepiante.ui.components.PlantPhoto
import com.lemiepiante.ui.components.SnoozeDialog
import com.lemiepiante.ui.components.icon
import com.lemiepiante.ui.components.urgencyColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: PlantDetailViewModel = viewModel(factory = AppViewModelFactory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var snoozeType by rememberSaveable { mutableStateOf<CareType?>(null) }
    var showFertilizeDialog by rememberSaveable { mutableStateOf(false) }
    var showDeletePlant by rememberSaveable { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<CareEvent?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }

    val plant = state.plant
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plant?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro") }
                },
                actions = {
                    if (plant != null) {
                        IconButton(onClick = { onEdit(plant.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Modifica") }
                        IconButton(onClick = { showDeletePlant = true }) { Icon(Icons.Filled.Delete, contentDescription = "Elimina") }
                    }
                },
            )
        },
    ) { padding ->
        if (plant == null || state.water == null || state.fertilize == null) {
            if (!state.loading) {
                Text("Pianta non trovata", modifier = Modifier.padding(padding).padding(16.dp))
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            item(key = "photo") {
                PlantPhoto(plant.photoPath, Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(20.dp)))
            }
            item(key = "info") {
                Column {
                    Text(plant.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    val subtitle = listOf(plant.species, plant.location).filter { it.isNotBlank() }.joinToString(" · ")
                    if (subtitle.isNotEmpty()) {
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item(key = "water") {
                CareCard(
                    status = state.water!!,
                    today = state.today,
                    doneLabel = "Innaffiata ora",
                    snoozeLabel = "Ancora umida",
                    onDone = { viewModel.markDone(CareType.WATER) },
                    onSnooze = { snoozeType = CareType.WATER },
                )
            }
            item(key = "fertilize") {
                CareCard(
                    status = state.fertilize!!,
                    today = state.today,
                    doneLabel = "Concimata ora",
                    snoozeLabel = "Rimanda",
                    onDone = { showFertilizeDialog = true },
                    onSnooze = { snoozeType = CareType.FERTILIZE },
                )
            }
            if (plant.notes.isNotBlank()) {
                item(key = "notes") {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Note", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(plant.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item(key = "history-title") {
                Text("Storico", style = MaterialTheme.typography.titleMedium)
            }
            if (state.events.isEmpty()) {
                item(key = "history-empty") {
                    Text(
                        "Ancora nessuna cura registrata.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.events, key = { "event-${it.id}" }) { event ->
                HistoryRow(event, onDelete = { eventToDelete = event })
                HorizontalDivider()
            }
        }
    }

    snoozeType?.let { type ->
        SnoozeDialog(
            type = type,
            onConfirm = { days ->
                viewModel.snooze(type, days)
                snoozeType = null
            },
            onDismiss = { snoozeType = null },
        )
    }

    if (showFertilizeDialog) {
        val last = state.lastFertilization
        FertilizeDialog(
            initialProduct = last?.product.orEmpty(),
            initialDose = last?.dose.orEmpty(),
            onConfirm = { product, dose ->
                viewModel.markDone(CareType.FERTILIZE, product, dose)
                showFertilizeDialog = false
            },
            onDismiss = { showFertilizeDialog = false },
        )
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Eliminare questa voce?") },
            text = { Text("${event.title()} del ${LocalDate.ofEpochDay(event.epochDay).longText()}. Il timer verrà ricalcolato.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEvent(event)
                    eventToDelete = null
                }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { eventToDelete = null }) { Text("Annulla") } },
        )
    }

    if (showDeletePlant && plant != null) {
        AlertDialog(
            onDismissRequest = { showDeletePlant = false },
            title = { Text("Eliminare ${plant.name}?") },
            text = { Text("Verranno eliminati anche foto e storico. L'operazione non si può annullare.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeletePlant = false
                    viewModel.deletePlant(onDeleted = onBack)
                }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { showDeletePlant = false }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun CareCard(
    status: CareStatus,
    today: LocalDate,
    doneLabel: String,
    snoozeLabel: String,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
) {
    val colors = urgencyColors(status.urgency)
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.container, contentColor = colors.content),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(status.type.icon(), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(status.type.label(), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(status.headline(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(status.details(today), style = MaterialTheme.typography.bodyMedium)
            if (status.urgency != Urgency.DISABLED) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onDone, modifier = Modifier.weight(1f)) {
                        Icon(status.type.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(doneLabel)
                    }
                    OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(snoozeLabel)
                    }
                }
            }
        }
    }
}

private fun CareStatus.details(today: LocalDate): String {
    if (urgency == Urgency.DISABLED) return "Sospesa nella stagione ${season.label()}"
    val parts = mutableListOf<String>()
    parts += lastDone?.let { "Ultima: ${it.shortText()} (${it.agoText(today)})" } ?: "Mai registrata"
    parts += "ogni $intervalDays gg (${season.label()})"
    if (snoozed && dueDate != null) parts += "rimandata al ${dueDate.shortText()}"
    return parts.joinToString(" · ")
}

private fun CareEvent.title(): String = when (kind) {
    EventKind.WATERED -> "Innaffiata"
    EventKind.FERTILIZED -> "Concimata"
    EventKind.WATER_SNOOZED -> "Acqua rimandata di $snoozeDays gg"
    EventKind.FERTILIZE_SNOOZED -> "Concime rimandato di $snoozeDays gg"
}

@Composable
private fun HistoryRow(event: CareEvent, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Icon(
            if (event.kind.isSnooze) Icons.Filled.Snooze else event.kind.type.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(event.title(), style = MaterialTheme.typography.bodyLarge)
            Text(
                LocalDate.ofEpochDay(event.epochDay).longText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val fertilizerInfo = listOf(event.product, event.dose).filter { it.isNotBlank() }.joinToString(" · ")
            if (fertilizerInfo.isNotEmpty()) {
                Text(fertilizerInfo, style = MaterialTheme.typography.bodySmall)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Elimina voce")
        }
    }
}
