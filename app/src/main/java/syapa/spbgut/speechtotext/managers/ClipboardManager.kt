package syapa.spbgut.speechtotext.managers

import android.content.ClipData
import syapa.spbgut.speechtotext.MainActivity
import syapa.spbgut.speechtotext.enums.LogMessages
import syapa.spbgut.speechtotext.loggers.Logger
import android.content.ClipboardManager as SystemClipboardManager

class ClipboardManager(
    private val activity: MainActivity, private val logger: Logger
) {

    companion object {
        private const val CLIP_LABEL = "Распознанный текст"
    }

    fun copyToClipboard(textToCopy: String) {
        if (textToCopy.isEmpty()) {
            logger.logWarning(
                LogMessages.COPY_TO_CLIPBOARD_ERROR
            )
            logger.showToast("Нет текста для копирования")
            return
        }

        try {
            val clipboard = activity.getSystemService(SystemClipboardManager::class.java)
            val clip = ClipData.newPlainText(CLIP_LABEL, textToCopy)
            clipboard.setPrimaryClip(clip)

            logger.logInfo(
                LogMessages.COPY_TO_CLIPBOARD,
                "Успешно скопировано. Длина: ${textToCopy.length} символов"
            )
            logger.showToast("Текст скопирован в буфер")

        } catch (e: SecurityException) {
            logger.handleError(
                LogMessages.COPY_TO_CLIPBOARD_ERROR, e, "SecurityException: ${e.message}"
            )
        } catch (e: Exception) {
            logger.handleError(
                LogMessages.COPY_TO_CLIPBOARD_ERROR,
                e,
                "Exception: ${e.localizedMessage ?: "неизвестная ошибка"}"
            )
        }
    }
}