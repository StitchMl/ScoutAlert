package it.agesci.scoutalert.excel

// Each row: values in order, aligned with headers
data class ExcelRow(
    val cells: List<String>
)

// Complete table
data class ExcelTable(
    val headers: List<String>,
    val rows: List<ExcelRow>
)

/**
 * Birthday info used for notifications.
 */
data class BirthdayEntry(
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val unit: String?,
    val day: Int,
    val month: Int,
    val year: Int?
)