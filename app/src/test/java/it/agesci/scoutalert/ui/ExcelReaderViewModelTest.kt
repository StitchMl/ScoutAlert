package it.agesci.scoutalert.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcelReaderViewModelTest {
    @Test
    fun `restores and updates saved UI state`() {
        val handle = SavedStateHandle(
            mapOf(
                "searchQuery" to "rossi",
                "selectedUnit" to "E/G",
                "isUploadOpen" to false,
                "isNotificationOpen" to true,
                "importedRows" to 8,
                "skippedRows" to 2,
                "isEditorOpen" to true,
                "editingIndex" to 3
            )
        )
        val viewModel = ExcelReaderViewModel(Application(), handle)

        assertEquals("rossi", viewModel.searchQuery.value)
        assertEquals("E/G", viewModel.selectedUnit.value)
        assertFalse(viewModel.isUploadOpen.value)
        assertTrue(viewModel.isNotificationOpen.value)
        assertEquals(8, viewModel.importedRows.value)
        assertEquals(2, viewModel.skippedRows.value)
        assertTrue(viewModel.isEditorOpen.value)
        assertEquals(3, viewModel.editingIndex.value)

        viewModel.setSearchQuery("")
        viewModel.setSelectedUnit(null)
        viewModel.setUploadOpen(true)
        viewModel.setNotificationOpen(false)
        viewModel.closeEditor()

        assertEquals("", handle.get<String>("searchQuery"))
        assertNull(handle.get<String>("selectedUnit"))
        assertTrue(handle.get<Boolean>("isUploadOpen") == true)
        assertTrue(handle.get<Boolean>("isNotificationOpen") == false)
        assertTrue(handle.get<Boolean>("isEditorOpen") == false)
        assertNull(handle.get<Int>("editingIndex"))
    }
}
