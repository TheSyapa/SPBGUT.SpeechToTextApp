package syapa.spbgut.speechtotext.listeners

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import syapa.spbgut.speechtotext.MainActivity
import syapa.spbgut.speechtotext.R
import syapa.spbgut.speechtotext.enums.LogMessages
import syapa.spbgut.speechtotext.loggers.Logger

class CustomRecognitionListener(
    private val activity: MainActivity, private val logger: Logger
) : RecognitionListener {

    override fun onReadyForSpeech(params: Bundle?) {
        logger.logInfo(LogMessages.RECOGNITION_READY)
        activity.runOnUiThread {
            activity.tvStatus.text = "Говорите..."
        }
    }

    override fun onBeginningOfSpeech() {
        logger.logDebug(LogMessages.RECOGNITION_BEGIN_SPEECH)
    }

    override fun onRmsChanged(rmsdB: Float) {
    }

    override fun onBufferReceived(buffer: ByteArray?) {
    }

    override fun onEndOfSpeech() {
        logger.logInfo(LogMessages.RECOGNITION_END_SPEECH)
        activity.runOnUiThread {
            activity.tvStatus.text = "Обработка..."
        }
    }

    override fun onError(error: Int) {
        val errorMsg = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> {
                logger.logError(LogMessages.RECOGNITION_AUDIO_ERROR)
                "Ошибка аудиозаписи"
            }

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                logger.logError(LogMessages.RECOGNITION_PERMISSION_ERROR, showToast = true)
                "Недостаточно прав"
            }

            SpeechRecognizer.ERROR_NETWORK -> {
                logger.logError(LogMessages.RECOGNITION_NETWORK_ERROR, showToast = true)
                "Проблемы с сетью"
            }

            SpeechRecognizer.ERROR_NO_MATCH -> {
                logger.logWarning(LogMessages.RECOGNITION_NO_MATCH)
                "Не удалось распознать речь"
            }

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                logger.logWarning(LogMessages.RECOGNITION_BUSY)
                "Речевой движок занят"
            }

            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                logger.logWarning(LogMessages.RECOGNITION_TIMEOUT)
                "Не обнаружена речь"
            }

            SpeechRecognizer.ERROR_CLIENT -> {
                logger.logWarning(LogMessages.RECOGNITION_CLIENT_ERROR)
                "Ошибка клиента (13)"
            }

            else -> {
                logger.logError(LogMessages.RECOGNITION_UNKNOWN_ERROR, formatArgs = arrayOf(error))
                "Неизвестная ошибка: $error"
            }
        }

        activity.runOnUiThread {
            "Ошибка: $errorMsg".also { activity.tvStatus.text = it }
            activity.isRecording = false
            activity.btnRecord.setImageResource(R.drawable.ic_mic)

            if (error == SpeechRecognizer.ERROR_CLIENT) {
                logger.logDebug(
                    LogMessages.RECORDING_STOP, "Перезапуск распознавателя из-за ERROR_CLIENT"
                )
                activity.speechRecognitionManager.resetSpeechRecognizer()
            }
        }
    }

    override fun onResults(results: Bundle?) {
        activity.runOnUiThread {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val recognizedText = matches[0] + " "
                    logger.logDebug(
                        LogMessages.RECOGNITION_SUCCESS_AMOUNT,
                        "${recognizedText.take(50)}${if (recognizedText.length > 50) "..." else ""}"
                    )

                    val currentText = activity.etResult.text.toString()
                    val beforeCursor = currentText.substring(0, activity.lastCursorPosition)
                    val afterCursor = currentText.substring(activity.lastCursorPosition)

                    val newText = "$beforeCursor$recognizedText$afterCursor"
                    activity.etResult.setText(newText)

                    val newCursorPosition = activity.lastCursorPosition + recognizedText.length
                    activity.etResult.setSelection(newCursorPosition)
                } else {
                    logger.logWarning(LogMessages.RECOGNITION_NO_MATCH)
                }
            }
            activity.isRecording = false
            activity.btnRecord.setImageResource(R.drawable.ic_mic)
            activity.tvStatus.text = "Готово. Нажмите кнопку для новой записи"
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        logger.logDebug(LogMessages.RECOGNITION_EVENT, formatArgs = arrayOf(eventType))
    }
}