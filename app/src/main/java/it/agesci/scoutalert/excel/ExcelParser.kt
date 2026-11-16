package it.agesci.scoutalert.excel

import android.content.Context
import android.net.Uri
import android.util.Log
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ExcelParser"
private const val MAX_IMPORTED_ROWS = 10_000

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

// ---------------------------------------------------
// EXCEL CELL DATA CONVERSION
// ---------------------------------------------------
private fun parseExcelDate(cell: Cell): LocalDate? {
    return try {
        if (DateUtil.isCellDateFormatted(cell)) {
            cell.dateCellValue.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        } else if (cell.cellType == CellType.NUMERIC) {
            val javaDate = DateUtil.getJavaDate(cell.numericCellValue)
            javaDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        } else null
    } catch (_: Exception) {
        null
    }
}

private fun parseStringDate(text: String): LocalDate? {
    val formats = listOf(
        "d/M/uuuu", "uuuu-MM-dd", "d-M-uuuu", "d.M.uuuu"
    )
    for (fmt in formats) {
        try {
            val formatter = DateTimeFormatter.ofPattern(fmt, Locale.getDefault())
                .withResolverStyle(ResolverStyle.STRICT)
            return LocalDate.parse(text.trim(), formatter)
        } catch (_: DateTimeParseException) { }
    }
    return null
}


// ---------------------------------------------------
// CELL FORMATION
// ---------------------------------------------------
private fun getCellValue(
    cell: Cell,
    evaluator: FormulaEvaluator,
    formatter: DataFormatter
): String {
    return try {
        formatter.formatCellValue(cell, evaluator).trim()
    } catch (_: Exception) {
        Log.w(TAG, "Cell error ${cell.rowIndex},${cell.columnIndex}")
        ""
    }
}


// ---------------------------------------------------
// MAIN PARSER
// ---------------------------------------------------
suspend fun parseExcelFromUri(context: Context, uri: Uri): ExcelTable =
    withContext(Dispatchers.IO) { parseExcelBlocking(context, uri) }

private fun parseExcelBlocking(context: Context, uri: Uri): ExcelTable {
    ExcelInputValidator.validate(context, uri)
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw ExcelParseException("Impossibile aprire il file selezionato")

    try {
        return inputStream.use { stream ->
            XSSFWorkbook(stream).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            val evaluator = workbook.creationHelper.createFormulaEvaluator()
            val formatter = DataFormatter()

            if (sheet.physicalNumberOfRows == 0) {
                throw ExcelParseException("Il primo foglio del file è vuoto")
            }

            val candidateHeaders = (0 until minOf(10, sheet.lastRowNum + 1)).map { rowIndex ->
                sheet.getRow(rowIndex)?.let { row ->
                    val lastColumn = row.lastCellNum.toInt().coerceAtLeast(0)
                    (0 until lastColumn).map { column ->
                        row.getCell(column)?.let { getCellValue(it, evaluator, formatter) }.orEmpty()
                    }
                }.orEmpty()
            }
            val header = detectExcelHeader(candidateHeaders)
            val columns = header.columns

            Log.d(TAG, "---- HEADER POSITIONS (NEW SMART MATCH) ----")
            Log.d(TAG, "Detected columns: $columns")
            Log.d(TAG, "------------------------------------------------")


            // ---- 2) BETWEEN THE LINES ----
            val rows = linkedSetOf<ExcelRow>()
            var skippedRows = 0
            var processedRows = 0

            for (rowIndex in (header.rowIndex + 1)..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                processedRows++
                if (processedRows > MAX_IMPORTED_ROWS) {
                    skippedRows++
                    continue
                }

                when (val result = parseRow(row, columns, evaluator, formatter)) {
                    RowParseResult.Blank -> Unit
                    RowParseResult.Invalid -> skippedRows++
                    is RowParseResult.Valid -> rows += result.row
                }
            }

            Log.d(TAG, "Rows imported=${rows.size}, skipped=$skippedRows")

            ExcelTable(
                headers = listOf("Cognome", "Nome", "Data", "Unità"),
                rows = rows.toList(),
                skippedRows = skippedRows
            )
            }
        }
    } catch (e: ExcelParseException) {
        throw e
    } catch (e: IllegalArgumentException) {
        throw ExcelParseException(e.message ?: "Intestazioni Excel non valide", e)
    } catch (e: Exception) {
        Log.e(TAG, "Excel parsing error", e)
        throw ExcelParseException("Il file non è un documento Excel valido", e)
    }
}

class ExcelParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal sealed interface RowParseResult {
    data object Blank : RowParseResult
    data object Invalid : RowParseResult
    data class Valid(val row: ExcelRow) : RowParseResult
}

private fun parseRow(
    row: Row,
    columns: ExcelColumns,
    evaluator: FormulaEvaluator,
    formatter: DataFormatter
): RowParseResult = try {
    fun field(index: Int?): String = index?.let { column ->
        row.getCell(column)?.let { getCellValue(it, evaluator, formatter) }
    }.orEmpty()

    val combinedName = field(columns.fullName)
    val (firstName, lastName) = if (columns.fullName != null) {
        splitFullName(combinedName)
    } else {
        field(columns.firstName) to field(columns.lastName)
    }
    val rawBirthDate = field(columns.birthDate)
    val unit = field(columns.unit)
    val birthCell = row.getCell(columns.birthDate)
    val excelBirthDate = birthCell?.let(::parseExcelDate)

    parseRowValues(lastName, firstName, rawBirthDate, unit, excelBirthDate)
} catch (error: RuntimeException) {
    Log.w(TAG, "Skipping invalid row ${row.rowNum + 1}", error)
    RowParseResult.Invalid
}

internal fun splitFullName(raw: String): Pair<String, String> {
    val tokens = raw.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return tokens.firstOrNull().orEmpty() to tokens.drop(1).joinToString(" ")
}

internal fun parseRowValues(
    lastNameRaw: String,
    firstNameRaw: String,
    rawBirthDate: String,
    unit: String,
    excelBirthDate: LocalDate? = null
): RowParseResult {
    val lastName = normalizeName(lastNameRaw)
    val firstName = normalizeName(firstNameRaw)

    if (lastName.isBlank() && firstName.isBlank() && rawBirthDate.isBlank() && unit.isBlank()) {
        return RowParseResult.Blank
    }

    val birthDate = excelBirthDate ?: parseStringDate(rawBirthDate)
    return if ((lastName.isBlank() && firstName.isBlank()) || birthDate == null) {
        RowParseResult.Invalid
    } else {
        RowParseResult.Valid(ExcelRow(listOf(lastName, firstName, birthDate.toString(), unit)))
    }
}
