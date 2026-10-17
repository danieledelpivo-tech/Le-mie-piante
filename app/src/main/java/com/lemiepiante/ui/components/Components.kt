package com.lemiepiante.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lemiepiante.domain.CareStatus
import com.lemiepiante.domain.CareType
import com.lemiepiante.domain.Urgency
import com.lemiepiante.domain.chipText
import java.io.File

data class UrgencyColors(val container: Color, val content: Color)

@Composable
fun urgencyColors(urgency: Urgency): UrgencyColors {
    val dark = isSystemInDarkTheme()
    return when (urgency) {
        Urgency.OVERDUE, Urgency.TODAY, Urgency.NEVER_DONE ->
            if (dark) UrgencyColors(Color(0xFF8C1D18), Color(0xFFFFDAD6)) else UrgencyColors(Color(0xFFFFDAD6), Color(0xFF8C1D18))
        Urgency.SOON ->
            if (dark) UrgencyColors(Color(0xFF5C4300), Color(0xFFFFE08A)) else UrgencyColors(Color(0xFFFFE8B0), Color(0xFF6B4E00))
        Urgency.OK ->
            if (dark) UrgencyColors(Color(0xFF1F4D27), Color(0xFFC4EBC2)) else UrgencyColors(Color(0xFFD3EFD0), Color(0xFF1F5F2A))
        Urgency.DISABLED ->
            UrgencyColors(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun CareType.icon(): ImageVector = when (this) {
    CareType.WATER -> Icons.Filled.WaterDrop
    CareType.FERTILIZE -> Icons.Filled.Spa
}

@Composable
fun StatusChip(status: CareStatus, modifier: Modifier = Modifier) {
    val colors = urgencyColors(status.urgency)
    Surface(color = colors.container, contentColor = colors.content, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Icon(status.type.icon(), contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(status.chipText(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun PlantPhoto(path: String?, modifier: Modifier = Modifier) {
    if (path != null) {
        AsyncImage(
            model = File(path),
            contentDescription = "Foto della pianta",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.LocalFlorist,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxSize(0.45f),
            )
        }
    }
}

/** Selettore di giorni con − e +; 0 significa "sospeso". */
@Composable
fun DaysStepper(label: String, value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onValueChange((value - 1).coerceAtLeast(0)) }, enabled = value > 0) {
            Icon(Icons.Filled.Remove, contentDescription = "Diminuisci")
        }
        Text(
            if (value == 0) "Sospeso" else "ogni $value gg",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 96.dp),
        )
        FilledTonalIconButton(onClick = { onValueChange((value + 1).coerceAtMost(365)) }) {
            Icon(Icons.Filled.Add, contentDescription = "Aumenta")
        }
    }
}

@Composable
fun SnoozeDialog(type: CareType, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(type.icon(), contentDescription = null) },
        title = { Text(if (type == CareType.WATER) "Terreno ancora umido?" else "Rimandare il concime?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Il timer non si azzera: il promemoria si sposta di qualche giorno.")
                listOf(1, 2, 3, 5, 7).forEach { days ->
                    TextButton(onClick = { onConfirm(days) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (days == 1) "Ricordamelo domani" else "Ricordamelo tra $days giorni")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
fun FertilizeDialog(
    initialProduct: String,
    initialDose: String,
    onConfirm: (product: String, dose: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var product by rememberSaveable { mutableStateOf(initialProduct) }
    var dose by rememberSaveable { mutableStateOf(initialDose) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Spa, contentDescription = null) },
        title = { Text("Concimata oggi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = product,
                    onValueChange = { product = it },
                    label = { Text("Prodotto (facoltativo)") },
                    placeholder = { Text("es. concime liquido universale") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it },
                    label = { Text("Dose (facoltativa)") },
                    placeholder = { Text("es. 5 ml in 1 L d'acqua") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(product, dose) }) { Text("Salva") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
