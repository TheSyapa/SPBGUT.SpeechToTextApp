package syapa.spbgut.speechtotext.managers

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import syapa.spbgut.speechtotext.MainActivity
import syapa.spbgut.speechtotext.R
import syapa.spbgut.speechtotext.enums.LogMessages
import syapa.spbgut.speechtotext.listeners.CustomRecognitionListener
import syapa.spbgut.speechtotext.loggers.Logger

class SpeechRecognitionManager(
    private val activity: MainActivity, private val logger: Logger
) {

    private lateinit var speechRecognizer: SpeechRecognizer

    init {
        initSpeechRecognizer()
    }

    fun initSpeechRecognizer() {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
                setRecognitionListener(CustomRecognitionListener(activity, logger))
            }
            logger.logInfo(LogMessages.RECOGNITION_READY, "Инициализирован речевой распознаватель")
        } catch (e: Exception) {
            logger.logError(
                LogMessages.INITIALIZATION_ERROR, e, "Не удалось создать SpeechRecognizer"
            )
        }
    }

    fun startRecording() {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            logger.logError(
                LogMessages.RECOGNITION_UNAVAILABLE, showToast = true
            )
            activity.runOnUiThread {
                logger.showToast(LogMessages.RECOGNITION_UNAVAILABLE, Toast.LENGTH_LONG)
            }
            return
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите сейчас...")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer.startListening(intent)
            activity.isRecording = true

            activity.runOnUiThread {
                activity.btnRecord.setImageResource(R.drawable.ic_stop)
                activity.tvStatus.text = "Слушаем..."
            }

            logger.logInfo(LogMessages.RECORDING_START)
        } catch (e: Exception) {
            logger.handleError(
                LogMessages.RECORDING_START_ERROR, e
            )
            activity.runOnUiThread {
                activity.isRecording = false
                activity.btnRecord.setImageResource(R.drawable.ic_mic)
                activity.tvStatus.text = "Ошибка при запуске"
            }
        }
    }

    fun stopRecording() {
        try {
            if (activity.isRecording && ::speechRecognizer.isInitialized) {
                speechRecognizer.stopListening()
                activity.isRecording = false
                logger.logInfo(LogMessages.RECORDING_STOP)

                activity.runOnUiThread {
                    activity.btnRecord.setImageResource(R.drawable.ic_mic)
                    activity.tvStatus.text = "Остановлено"
                }
            }
        } catch (e: Exception) {
            logger.logError(
                LogMessages.RECORDING_STOP_ERROR, e
            )
        }
    }

    fun resetSpeechRecognizer() {
        try {
            if (::speechRecognizer.isInitialized) {
                speechRecognizer.destroy()
                logger.logDebug(
                    LogMessages.RECOGNIZER_RESET, "SpeechRecognizer уничтожен"
                )
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
                setRecognitionListener(CustomRecognitionListener(activity, logger))
            }
            logger.logInfo(LogMessages.RECOGNIZER_RESET, "SpeechRecognizer пересоздан")
        } catch (e: Exception) {
            logger.logError(
                LogMessages.INITIALIZATION_ERROR, e, "Не удалось сбросить SpeechRecognizer"
            )
        }
    }

    fun destroy() {
        try {
            if (::speechRecognizer.isInitialized) {
                speechRecognizer.destroy()
                logger.logInfo(LogMessages.RECOGNIZER_DESTROYED)
            }
        } catch (e: Exception) {
            logger.logError(
                LogMessages.DESTROY_ERROR, e, "Ошибка при уничтожении SpeechRecognizer"
            )
        }
    }
}