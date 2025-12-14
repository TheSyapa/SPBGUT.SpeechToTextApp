package syapa.spbgut.speechtotext.loggers

import android.content.Context
import android.util.Log
import android.widget.Toast
import syapa.spbgut.speechtotext.enums.LogMessages

class Logger(
    private var tag: String,
    private val context: Context? = null,
    private val showToastsInProduction: Boolean = true
) {

    init {
        if (!tag.startsWith("APP_STT_")) {
            tag = "APP_STT_$tag"
        }
    }

    /**
     * Обрабатывает ошибку с логом и Toast (если задан в enum и showToastsInProduction = true)
     */
    fun handleError(
        logMessage: LogMessages, exception: Exception, vararg formatArgs: Any?
    ) {
        logError(logMessage, exception, *formatArgs, showToast = true)
    }

    /**
     * Логирует ошибку с возможностью управления показом Toast
     */
    fun logError(
        logMessage: LogMessages,
        exception: Exception? = null,
        vararg formatArgs: Any?,
        showToast: Boolean = true
    ) {
        val formattedLogText = formatLogText(logMessage.logText, *formatArgs)

        if (exception != null) {
            Log.e(tag, formattedLogText, exception)
        } else {
            Log.e(tag, formattedLogText)
        }

        if (showToast && showToastsInProduction && logMessage.toastText != null && context != null) {
            val toastText = logMessage.toastText
            if (formatArgs.isNotEmpty() && toastText.contains("%")) {
                // Форматируем текст тоста, если есть плейсхолдеры
                val formattedToastText = String.format(toastText, *formatArgs)
                Toast.makeText(context, formattedToastText, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Логирует информационное сообщение
     */
    fun logInfo(logMessage: LogMessages, vararg formatArgs: Any?) {
        val formattedText = formatLogText(logMessage.logText, *formatArgs)
        Log.i(tag, formattedText)
    }

    /**
     * Логирует отладочное сообщение
     */
    fun logDebug(logMessage: LogMessages, vararg formatArgs: Any?) {
        val formattedText = formatLogText(logMessage.logText, *formatArgs)
        Log.d(tag, formattedText)
    }

    /**
     * Логирует предупреждение
     */
    fun logWarning(logMessage: LogMessages, exception: Exception? = null, vararg formatArgs: Any?) {
        val formattedText = formatLogText(logMessage.logText, *formatArgs)
        if (exception != null) {
            Log.w(tag, formattedText, exception)
        } else {
            Log.w(tag, formattedText)
        }
    }

    /**
     * Форматирует текст лога, заменяя плейсхолдеры
     */
    private fun formatLogText(text: String, vararg formatArgs: Any?): String {
        return if (formatArgs.isNotEmpty() && text.contains("%")) {
            try {
                String.format(text, *formatArgs)
            } catch (e: Exception) {
                "$text [Ошибка форматирования: ${e.message}]"
            }
        } else {
            text
        }
    }

    /**
     * Показывает Toast сообщение отдельно от логирования
     */
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        if (context != null && showToastsInProduction) {
            Toast.makeText(context, message, duration).show()
        }
    }

    /**
     * Показывает Toast из перечисления LogMessages
     */
    fun showToast(logMessage: LogMessages, vararg formatArgs: Any?) {
        if (context != null && showToastsInProduction && logMessage.toastText != null) {
            val toastText = logMessage.toastText
            if (formatArgs.isNotEmpty() && toastText.contains("%")) {
                val formattedToastText = String.format(toastText, *formatArgs)
                Toast.makeText(context, formattedToastText, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
            }
        }
    }
}