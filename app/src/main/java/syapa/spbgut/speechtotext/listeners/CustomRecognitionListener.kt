package syapa.spbgut.speechtotext.listeners

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import syapa.spbgut.speechtotext.MainActivity
import syapa.spbgut.speechtotext.R

class CustomRecognitionListener(private val activity: MainActivity) : RecognitionListener {

    private val TAG = "SpeechToTextApp"

    override fun onReadyForSpeech(params: Bundle?) {
        activity.runOnUiThread {
            activity.tvStatus.text = "Говорите..."
            android.util.Log.d(TAG, "onReadyForSpeech")
        }
    }

    override fun onBeginningOfSpeech() {
        android.util.Log.d(TAG, "onBeginningOfSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        activity.runOnUiThread {
            activity.tvStatus.text = "Обработка..."
            android.util.Log.d(TAG, "onEndOfSpeech")
        }
    }

    override fun onError(error: Int) {
        activity.runOnUiThread {
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Ошибка аудиозаписи"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Недостаточно прав"
                SpeechRecognizer.ERROR_NETWORK -> "Проблемы с сетью"
                SpeechRecognizer.ERROR_NO_MATCH -> "Не удалось распознать речь"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Речевой движок занят"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Не обнаружена речь"
                SpeechRecognizer.ERROR_CLIENT -> "Ошибка клиента (13)"
                else -> "Неизвестная ошибка: $error"
            }

            "Ошибка: $errorMsg".also { activity.tvStatus.text = it }
            activity.isRecording = false
            activity.btnRecord.setImageResource(R.drawable.ic_mic)
            android.util.Log.e(TAG, "Recognition error: $errorMsg")

            if (error == SpeechRecognizer.ERROR_CLIENT) {
                activity.speechRecognitionManager.resetSpeechRecognizer()
            }
        }
    }

    override fun onResults(results: Bundle?) {
        activity.runOnUiThread {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.let { matches ->
                    if (matches.isNotEmpty()) {
                        val recognizedText = matches[0] + " "

                        val currentText = activity.etResult.text.toString()
                        val beforeCursor = currentText.substring(0, activity.lastCursorPosition)
                        val afterCursor = currentText.substring(activity.lastCursorPosition)

                        val newText = "$beforeCursor$recognizedText$afterCursor"
                        activity.etResult.setText(newText)

                        val newCursorPosition = activity.lastCursorPosition + recognizedText.length
                        activity.etResult.setSelection(newCursorPosition)
                    }
                }
            activity.isRecording = false
            activity.btnRecord.setImageResource(R.drawable.ic_mic)
            activity.tvStatus.text = "Готово. Нажмите кнопку для новой записи"
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}