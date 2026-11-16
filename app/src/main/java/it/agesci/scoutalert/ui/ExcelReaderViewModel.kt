package it.agesci.scoutalert.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import it.agesci.scoutalert.excel.ExcelParseException
import it.agesci.scoutalert.excel.ExcelTable
import it.agesci.scoutalert.excel.parseExcelFromUri
import it.agesci.scoutalert.excel.withManualUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExcelReaderViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    val searchQuery = savedStateHandle.getStateFlow(SEARCH_QUERY, "")
    val selectedUnit = savedStateHandle.getStateFlow<String?>(SELECTED_UNIT, null)
    val isUploadOpen = savedStateHandle.getStateFlow(IS_UPLOAD_OPEN, true)
    val isNotificationOpen = savedStateHandle.getStateFlow(IS_NOTIFICATION_OPEN, false)
    val importedRows = savedStateHandle.getStateFlow(IMPORTED_ROWS, 0)
    val skippedRows = savedStateHandle.getStateFlow(SKIPPED_ROWS, 0)
    val isEditorOpen = savedStateHandle.getStateFlow(IS_EDITOR_OPEN, false)
    val editingIndex = savedStateHandle.getStateFlow<Int?>(EDITING_INDEX, null)
    val manualUnit = savedStateHandle.getStateFlow(MANUAL_UNIT, "")

    private val _table = MutableStateFlow<ExcelTable?>(null)
    val table: StateFlow<ExcelTable?> = _table.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _errorMessage = MutableStateFlow(savedStateHandle.get<String>(ERROR_MESSAGE))
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    private var importJob: Job? = null

    init {
        if (!savedStateHandle.contains(IS_UPLOAD_OPEN)) {
            val hasSavedData = application
                .getSharedPreferences(PREFS_NAME, 0)
                .getString(BIRTHDAYS_KEY, null)
                .isNullOrBlank()
                .not()
            setUploadOpen(!hasSavedData)
        }
    }

    fun setSearchQuery(value: String) = savedStateHandle.set(SEARCH_QUERY, value)
    fun setSelectedUnit(value: String?) = savedStateHandle.set(SELECTED_UNIT, value)
    fun setUploadOpen(value: Boolean) = savedStateHandle.set(IS_UPLOAD_OPEN, value)
    fun setNotificationOpen(value: Boolean) = savedStateHandle.set(IS_NOTIFICATION_OPEN, value)
    fun setManualUnit(value: String) {
        savedStateHandle[MANUAL_UNIT] = value
        _table.value = _table.value?.withManualUnit(value)
    }
    fun openEditor(index: Int?) {
        savedStateHandle[EDITING_INDEX] = index
        savedStateHandle[IS_EDITOR_OPEN] = true
    }

    fun closeEditor() {
        savedStateHandle[IS_EDITOR_OPEN] = false
        savedStateHandle[EDITING_INDEX] = null
    }

    fun importFile(uri: Uri) {
        savedStateHandle[LAST_URI] = uri.toString()
        importJob?.cancel()
        importJob = viewModelScope.launch {
            _isLoading.value = true
            setError(null)
            try {
                val parsed = parseExcelFromUri(getApplication(), uri, manualUnit.value)
                if (parsed.rows.isEmpty()) {
                    setError("Il file non contiene compleanni validi.")
                    setUploadOpen(true)
                } else {
                    _table.value = parsed
                    savedStateHandle[IMPORTED_ROWS] = parsed.rows.size
                    savedStateHandle[SKIPPED_ROWS] = parsed.skippedRows
                    setUploadOpen(false)
                    setNotificationOpen(false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: ExcelParseException) {
                setError(error.message ?: "Impossibile leggere il file Excel.")
                setUploadOpen(true)
            } catch (_: Exception) {
                setError("Impossibile leggere il file Excel.")
                setUploadOpen(true)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retryLastImport(): Boolean {
        val uri = savedStateHandle.get<String>(LAST_URI)?.let(Uri::parse) ?: return false
        importFile(uri)
        return true
    }

    fun dismissError() = setError(null)

    private fun setError(message: String?) {
        _errorMessage.value = message
        savedStateHandle[ERROR_MESSAGE] = message
    }

    private companion object {
        const val SEARCH_QUERY = "searchQuery"
        const val SELECTED_UNIT = "selectedUnit"
        const val IS_UPLOAD_OPEN = "isUploadOpen"
        const val IS_NOTIFICATION_OPEN = "isNotificationOpen"
        const val ERROR_MESSAGE = "errorMessage"
        const val LAST_URI = "lastImportUri"
        const val IMPORTED_ROWS = "importedRows"
        const val SKIPPED_ROWS = "skippedRows"
        const val IS_EDITOR_OPEN = "isEditorOpen"
        const val EDITING_INDEX = "editingIndex"
        const val MANUAL_UNIT = "manualUnit"
        const val PREFS_NAME = "birthdays_prefs"
        const val BIRTHDAYS_KEY = "birthdays_json"
    }
}
