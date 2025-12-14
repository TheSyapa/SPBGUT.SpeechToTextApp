package syapa.spbgut.speechtotext.managers

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import syapa.spbgut.speechtotext.MainActivity
import syapa.spbgut.speechtotext.enums.LogMessages
import syapa.spbgut.speechtotext.loggers.Logger
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class FileManager(
    private val activity: MainActivity, private val logger: Logger
) {

    companion object {
        private const val FILE_TYPE = "text/plain"
        private const val DEFAULT_FILENAME_PREFIX = "voice_text_"
        private const val FILE_EXTENSION = ".txt"
    }

    private var createDocumentLauncher: ActivityResultLauncher<String>? = null
    private var textToSave: String = ""

    fun initialize() {
        createDocumentLauncher = activity.registerForActivityResult(
            ActivityResultContracts.CreateDocument(FILE_TYPE)
        ) { uri ->
            if (uri != null) {
                saveTextToUri(uri)
            } else {
                logger.logWarning(
                    LogMessages.SAVE_TO_FILE_ERROR
                )
            }
        }
        logger.logInfo(LogMessages.SAVE_TO_FILE, "FileManager успешно инициализирован")
    }

    fun saveToFile(text: String) {
        if (text.isEmpty()) {
            logger.logWarning(
                LogMessages.SAVE_EMPTY_TEXT
            )
            logger.showToast("Текст пуст")
            return
        }

        textToSave = text

        val fileName = "${DEFAULT_FILENAME_PREFIX}${System.currentTimeMillis()}$FILE_EXTENSION"

        try {
            createDocumentLauncher?.launch(fileName)
        } catch (e: Exception) {
            logger.handleError(
                LogMessages.SAVE_TO_FILE_ERROR, e, "Не удалось запустить диалог сохранения файла"
            )
        }
    }

    private fun saveTextToUri(uri: Uri) {
        try {
            activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write(textToSave)
                    writer.flush()

                    logger.logInfo(
                        LogMessages.SAVE_TO_FILE,
                        "Файл успешно сохранён. URI: $uri, Размер: ${textToSave.length} символов"
                    )
                    logger.showToast("Файл сохранен")
                }
            } ?: run {
                logger.logError(
                    LogMessages.SAVE_TO_FILE_ERROR, showToast = true
                )
            }
        } catch (e: SecurityException) {
            logger.handleError(
                LogMessages.SAVE_TO_FILE_ERROR, e, "SecurityException при сохранении файла"
            )
        } catch (e: Exception) {
            logger.handleError(
                LogMessages.SAVE_TO_FILE_ERROR, e, "Ошибка при сохранении файла в URI: $uri"
            )
        } finally {
            textToSave = ""
        }
    }

    fun destroy() {
        logger.logDebug(LogMessages.SAVE_TO_FILE, "FileManager уничтожен")
    }
}