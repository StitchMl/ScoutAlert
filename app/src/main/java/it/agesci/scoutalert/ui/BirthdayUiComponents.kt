package it.agesci.scoutalert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate

// UI model used by the screen
data class PersonUi(
    val displayName: String,
    val unit: String?,
    val date: LocalDate?,
    val dateLabel: String,
    val index: Int        // <-- index in the BirthdayEntry list
)

@Composable
fun BirthdayHeroHeader(
    todayBirthdaysCount: Int,
    todayBirthdayLines: List<String>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                ),
                shape = CardDefaults.shape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column {
                Text(
                    text = "Compleanni in vista 🎉",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                )

                val subtitle = when {
                    todayBirthdaysCount <= 0 ->
                        "Nessun compleanno oggi"
                    todayBirthdaysCount == 1 ->
                        "Oggi c'è 1 compleanno nelle unità selezionate"
                    else ->
                        "Oggi ci sono $todayBirthdaysCount compleanni nelle unità selezionate"
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Names of today's birthday boys and girls (max. 3 lines for safety reasons)
                if (todayBirthdaysCount > 0) {
                    Text(
                        text = todayBirthdayLines.take(3).joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun FilePickerCard(
    isLoading: Boolean,
    onPickFile: () -> Unit,
    lastFileLoaded: String?
) {
    val isFirstTime = lastFileLoaded == null

    if (isFirstTime) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Carica l'Excel dei censiti",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = "Scegli il file Excel del censimento AGESCI: l'app estrarrà nome, cognome, " +
                            "data di nascita e unità, e programmerà le notifiche di compleanno.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = onPickFile,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isLoading) "Caricamento..." else "Seleziona file")
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "File caricato",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = lastFileLoaded,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    TextButton(
                        onClick = onPickFile,
                        enabled = !isLoading,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Ricarica", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationSettingsCard(
    availableUnits: List<String>,
    unitsWithNotifications: Set<String>,
    onToggleUnit: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Notifiche per unità",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Text(
                text = "Scegli per quali unità ricevere la notifica del compleanno:",
                style = MaterialTheme.typography.bodySmall
            )

            if (availableUnits.isEmpty()) {
                Text(
                    text = "Carica un file per vedere le unità.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableUnits.forEach { unit ->
                        val isSelected = unitsWithNotifications.contains(unit)

                        AssistChip(
                            onClick = { onToggleUnit(unit) },
                            label = { Text(unit) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor =
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface,
                                labelColor =
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null,
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (unitsWithNotifications.isEmpty()) {
                        TextButton(onClick = {
                            availableUnits.forEach { onToggleUnit(it) }
                        }) {
                            Text("Seleziona tutto")
                        }
                    } else {
                        TextButton(onClick = {
                            availableUnits.forEach {
                                if (unitsWithNotifications.contains(it))
                                    onToggleUnit(it)
                            }
                        }) {
                            Text("Deseleziona tutto")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedUnit: String?,
    availableUnits: List<String>,
    onUnitSelected: (String?) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Cerca per nome o unità") },
            singleLine = true
        )

        if (availableUnits.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Filtro unità",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AssistChip(
                    onClick = { onUnitSelected(null) },
                    label = { Text("Tutte") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selectedUnit == null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surface,
                        labelColor = if (selectedUnit == null)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                )

                availableUnits.forEach { unit ->
                    AssistChip(
                        onClick = {
                            onUnitSelected(
                                if (selectedUnit == unit) null else unit
                            )
                        },
                        label = { Text(unit) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedUnit == unit)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surface,
                            labelColor = if (selectedUnit == unit)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BirthdayListCard(
    persons: List<PersonUi>,
    onPersonClick: (PersonUi) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Compleanni trovati (${persons.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(8.dp)
            )

            if (persons.isEmpty()) {
                Text(
                    text = "Nessun risultato con i filtri correnti.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .padding(bottom = 23.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = persons,
                        key = { person -> person.index }
                    ) { person ->
                        BirthdayRow(
                            person = person,
                            onClick = { onPersonClick(person) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BirthdayRow(
    person: PersonUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = person.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = person.unit ?: "Unità sconosciuta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = person.dateLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "🎂",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Nessun compleanno salvato",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Carica un file Excel dal censimento per visualizzare l'elenco dei compleanni e attivare le notifiche per le unità.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Errore",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
                if (onRetry != null) {
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Riprova")
                    }
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Chiudi errore",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun ImportSummaryCard(
    importedRows: Int,
    skippedRows: Int,
    onRetry: () -> Unit
) {
    val hasSkippedRows = skippedRows > 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(
            containerColor = if (hasSkippedRows) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (hasSkippedRows) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (hasSkippedRows) "Importazione parziale" else "Importazione completata",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "$importedRows righe importate, $skippedRows ignorate.",
                style = MaterialTheme.typography.bodyMedium
            )
            if (hasSkippedRows) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Scegli un altro file")
                }
            }
        }
    }
}
