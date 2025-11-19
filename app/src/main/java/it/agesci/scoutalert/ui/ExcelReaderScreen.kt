package it.agesci.scoutalert.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import it.agesci.scoutalert.excel.BirthdayEntry
import it.agesci.scoutalert.excel.ExcelTable
import it.agesci.scoutalert.excel.parseExcelFromUri
import it.agesci.scoutalert.notifications.BirthdayScheduler
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Suppress("KotlinConstantConditions")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelReaderScreen() {
    val context = LocalContext.current

    // Main status
    var table by remember { mutableStateOf<ExcelTable?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf<String?>(null) }

    // main list of birthdays (editable) – load from SharedPreferences on first composition
    var birthdayEntries by remember {
        mutableStateOf(loadBirthdaysFromPrefs(context))
    }

    // panels
    // If there are NO saved birthdays -> upload open
    // If there is already saved data -> upload closed
    var isUploadOpen by remember { mutableStateOf(birthdayEntries.isEmpty()) }
    var isNotificationOpen by remember { mutableStateOf(false) }

    // notification preferences
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var unitsWithNotifications by remember {
        mutableStateOf(
            prefs.getStringSet(PREFS_UNITS_KEY, emptySet()) ?: emptySet()
        )
    }

    // dialog editor status
    var isEditorOpen by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    // Launcher per l'Excel
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        try {
            isLoading = true
            errorMessage = null
            val parsed = parseExcelFromUri(context, uri)
            table = parsed
            isUploadOpen = false
            isNotificationOpen = false
        } catch (t: Throwable) {
            Log.e(TAG, "Errore nel parse dell'Excel", t)
            errorMessage = "Impossibile leggere il file Excel."
            table = null
        } finally {
            isLoading = false
        }
    }

    // When the table changes from Excel, I regenerate BirthdayEntry and save it.
    LaunchedEffect(table) {
        val currentTable = table ?: return@LaunchedEffect

        val lastNameIndex = 0
        val firstNameIndex = 1
        val birthDateIndex = 2
        val unitIndex = if (currentTable.headers.size > 3) 3 else null

        Log.d(
            TAG,
            "UI indices -> lastName=$lastNameIndex, firstName=$firstNameIndex, " +
                    "birth=$birthDateIndex, unit=$unitIndex"
        )
        Log.d(TAG, "UI headers  -> ${currentTable.headers.joinToString()}")

        val entries = currentTable.rows.map { row ->
            val lastNameRaw = normalizeName(row.cells.getOrNull(lastNameIndex).orEmpty())
            val firstNameRaw = normalizeName(row.cells.getOrNull(firstNameIndex).orEmpty())

            val birthRaw = row.cells.getOrNull(birthDateIndex).orEmpty()
            val date = parseBirthDate(birthRaw)

            val unitRaw = unitIndex?.let { row.cells.getOrNull(it) }
            val unitNorm = unitRaw?.let { normalizeUnitName(it) }

            BirthdayEntry(
                firstName = firstNameRaw,
                lastName = lastNameRaw,
                fullName = listOf(firstNameRaw, lastNameRaw)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifEmpty { "Senza nome" },

                unit = unitNorm,
                day = date?.dayOfMonth ?: 0,
                month = date?.monthValue ?: 0,
                year = date?.year
            )
        }

        birthdayEntries = entries
        saveBirthdaysToPrefs(context, entries)
    }

    // UI model based on BirthdayEntries (including manual entries)
    val persons: List<PersonUi> = remember(birthdayEntries) {
        birthdayEntries.mapIndexed { index, entry ->
            val displayName = listOf(entry.lastName, entry.firstName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifEmpty { "Senza nome" }

            val date = if (entry.year != null) {
                try {
                    LocalDate.of(entry.year, entry.month, entry.day)
                } catch (_: Exception) {
                    null
                }
            } else null

            val label = date?.format(DateTimeFormatter.ofPattern("dd/MM"))
                ?: "%02d/%02d".format(entry.day, entry.month)

            PersonUi(
                displayName = displayName,
                unit = entry.unit,
                date = date,
                dateLabel = label,
                index = index
            )
        }
    }

    // filter and notification unit
    val availableUnits: List<String> = remember(persons) {
        persons.mapNotNull { it.unit }
            .distinct()
            .sorted()
    }

    // filtered/sorted list
    val filteredPersons: List<PersonUi> = remember(
        persons,
        searchQuery,
        selectedUnit
    ) {
        val q = searchQuery.trim().lowercase()

        persons
            .asSequence()
            .filter { p ->
                if (q.isBlank()) true
                else {
                    p.displayName.lowercase().contains(q) ||
                            (p.unit?.lowercase()?.contains(q) == true)
                }
            }
            .filter { p ->
                selectedUnit.isNullOrEmpty() || p.unit == selectedUnit
            }
            .sortedWith { p1, p2 ->
                val d1 = p1.date
                val d2 = p2.date
                when {
                    d1 == null && d2 == null -> p1.displayName.compareTo(p2.displayName)
                    d1 == null -> 1
                    d2 == null -> -1
                    else -> {
                        val m = d1.monthValue.compareTo(d2.monthValue)
                        if (m != 0) m
                        else {
                            val day = d1.dayOfMonth.compareTo(d2.dayOfMonth)
                            if (day != 0) day
                            else d1.year.compareTo(d2.year)
                        }
                    }
                }
            }
            .toList()
    }

    // Today's birthdays (independent of search/unit filter, but filtered on units with notification)
    val todayBirthdaysForUnits = remember(persons, unitsWithNotifications) {
        val today = LocalDate.now()
        persons.filter { p ->
            val d = p.date ?: return@filter false
            val matchesDate =
                d.dayOfMonth == today.dayOfMonth && d.monthValue == today.monthValue

            val matchesUnit =
                unitsWithNotifications.isEmpty() ||
                        (p.unit != null && unitsWithNotifications.contains(p.unit))

            matchesDate && matchesUnit
        }
    }

    val todayBirthdaysCount = todayBirthdaysForUnits.size

    val todayBirthdayLines = remember(todayBirthdaysForUnits) {
        todayBirthdaysForUnits.map { person ->
            buildString {
                append(person.displayName)
                person.unit?.let { append(" (").append(it).append(")") }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bday Alert") },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = null
                    )
                },
                actions = {

                    // UPLOAD BUTTON
                    IconButton(onClick = {
                        isUploadOpen = !isUploadOpen
                        Log.d(TAG, "Upload open: $isUploadOpen")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = if (isUploadOpen)
                                "Nascondi Upload" else "Mostra Upload",
                            tint = if (isUploadOpen)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // NOTIFICATION BUTTON
                    IconButton(onClick = {
                        isNotificationOpen = !isNotificationOpen
                        Log.d(TAG, "Notification open: $isNotificationOpen")
                    }) {
                        Icon(
                            imageVector = Icons.Default.NotificationAdd,
                            contentDescription = if (isNotificationOpen)
                                "Nascondi notifiche" else "Mostra notifiche",
                            tint = if (isNotificationOpen)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingIndex = null
                    isEditorOpen = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aggiungi compleanno"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            BirthdayHeroHeader(
                todayBirthdaysCount = todayBirthdaysCount,
                todayBirthdayLines = todayBirthdayLines
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // UPLOAD
                if (isUploadOpen) {
                    FilePickerCard(
                        isLoading = isLoading,
                        onPickFile = { launcher.launch("*/*") },
                        lastFileLoaded = if (birthdayEntries.isNotEmpty()) "Dati salvati" else null
                    )

                    if (errorMessage != null) {
                        ErrorCard(errorMessage!!)
                    }
                }

                // NOTIFICATIONS
                if (isNotificationOpen) {
                    NotificationSettingsCard(
                        availableUnits = availableUnits,
                        unitsWithNotifications = unitsWithNotifications,
                        onToggleUnit = { unit ->
                            val current = unitsWithNotifications.toMutableSet()
                            if (current.contains(unit)) {
                                current.remove(unit)
                            } else {
                                current.add(unit)
                            }
                            unitsWithNotifications = current
                            prefs.edit { putStringSet(PREFS_UNITS_KEY, current) }

                            // Update widget + notification IMMEDIATELY based on new units
                            BirthdayScheduler.triggerNow(context)
                        }
                    )
                }

                // LIST + FILTERS
                if (persons.isNotEmpty()) {
                    FilterBar(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedUnit = selectedUnit,
                        availableUnits = availableUnits,
                        onUnitSelected = { selectedUnit = it }
                    )

                    BirthdayListCard(
                        persons = filteredPersons,
                        onPersonClick = { person ->
                            editingIndex = person.index
                            isEditorOpen = true
                        }
                    )
                } else {
                    EmptyStateCard()
                }
            }
        }
    }

    // Add/edit birthday dialog
    if (isEditorOpen) {
        val initial = editingIndex?.let { idx -> birthdayEntries.getOrNull(idx) }
        BirthdayEditorDialog(
            title = if (initial == null) "Nuovo compleanno" else "Modifica compleanno",
            availableUnits = availableUnits,
            initial = initial,
            onDismiss = {
                isEditorOpen = false
                editingIndex = null
                Log.d(TAG, "Editor open: $isEditorOpen")
                Log.d(TAG, "Editing index: $editingIndex")
            },
            onSave = { newEntry ->
                val updated = birthdayEntries.toMutableList()
                if (editingIndex == null) {
                    updated.add(newEntry)
                } else {
                    val idx = editingIndex!!
                    if (idx in updated.indices) {
                        updated[idx] = newEntry
                    } else {
                        updated.add(newEntry)
                    }
                }
                birthdayEntries = updated
                saveBirthdaysToPrefs(context, updated)
                isEditorOpen = false
                editingIndex = null
                Log.d(TAG, "Editor open: $isEditorOpen")
                Log.d(TAG, "Editing index: $editingIndex")
            },
            onDelete = { entry ->
                val updated = birthdayEntries.toMutableList()
                updated.remove(entry)
                birthdayEntries = updated
                saveBirthdaysToPrefs(context, updated)
                isEditorOpen = false
                editingIndex = null
                Log.d(TAG, "Editor open: $isEditorOpen")
                Log.d(TAG, "Editing index: $editingIndex")
            }
        )
    }
}

/**
 * Dialog box for entering/editing a birthday.
 */
@Composable
fun BirthdayEditorDialog(
    title: String,
    availableUnits: List<String>,
    initial: BirthdayEntry?,
    onDismiss: () -> Unit,
    onSave: (BirthdayEntry) -> Unit,
    onDelete: (BirthdayEntry) -> Unit
) {
    Log.d(TAG, "Available units: $availableUnits")
    var firstName by remember { mutableStateOf(initial?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initial?.lastName ?: "") }
    var unit by remember { mutableStateOf(initial?.unit ?: "") }
    var dayText by remember { mutableStateOf(initial?.day?.toString() ?: "") }
    var monthText by remember { mutableStateOf(initial?.month?.toString() ?: "") }
    var yearText by remember { mutableStateOf(initial?.year?.toString() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Cognome") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unità (es. L/C)") },
                    singleLine = true
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = dayText,
                        onValueChange = { dayText = it },
                        label = { Text("Giorno") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthText,
                        onValueChange = { monthText = it },
                        label = { Text("Mese") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it },
                        label = { Text("Anno") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val day = dayText.toIntOrNull()
                val month = monthText.toIntOrNull()
                val year = yearText.toIntOrNull()
                if (day == null || month == null || year == null) {
                    error = "Inserisci una data valida (gg/mm/aaaa)"
                    Log.e(TAG, "$error: $dayText/$monthText/$yearText")
                    return@TextButton
                }
                try {
                    LocalDate.of(year, month, day)
                } catch (_: Exception) {
                    error = "Data non valida"
                    Log.e(TAG, "$error: $day/$month/$year")
                    return@TextButton
                }
                val entry = BirthdayEntry(
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    fullName = listOf(firstName.trim(), lastName.trim())
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifEmpty { "Senza nome" },
                    unit = unit.trim().ifBlank { null },
                    day = day,
                    month = month,
                    year = year
                )
                onSave(entry)
            }) {
                Text("Salva")
            }
        },
        dismissButton = {
            Row {
                // ❌ delete
                if (initial != null) {
                    TextButton(onClick = {
                        onDelete(initial)
                    }) {
                        Text("Elimina", color = MaterialTheme.colorScheme.error)
                    }
                }

                // 🔙 annulla
                TextButton(onClick = onDismiss) {
                    Text("Annulla")
                }
            }
        }
    )
}

private fun normalizeName(raw: String): String {
    return raw
        .trim()
        .lowercase()
        .split(Regex("\\s+"))
        .joinToString(" ") { part ->
            part.replaceFirstChar { c ->
                if (c.isLetter()) c.titlecase(Locale.ROOT) else c.toString()
            }
        }
}