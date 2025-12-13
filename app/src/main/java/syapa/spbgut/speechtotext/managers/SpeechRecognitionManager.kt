package syapa.spbgut.speechtotext.managers

import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import syapa.spbgut.speechtotext.MainActivity
import syapa.spbgut.speechtotext.R
import syapa.spbgut.speechtotext.listeners.CustomRecognitionListener

class SpeechRecognitionManager(private val activity: MainActivity) {

    private lateinit var speechRecognizer: SpeechRecognizer
    private val TAG = "SpeechToTextApp"

    init {
        initSpeechRecognizer()
    }

    fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
            setRecognitionListener(CustomRecognitionListener(activity))
        }
    }

    fun startRecording() {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            Toast.makeText(activity, "Распознавание речи недоступно", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите сейчас...")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            }

            speechRecognizer.startListening(intent)
            activity.isRecording = true
            activity.btnRecord.setImageResource(R.drawable.ic_stop)
            activity.tvStatus.text = "Слушаем..."
            android.util.Log.d(TAG, "Recording started")
        } catch (e: Exception) {
            Toast.makeText(activity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e(TAG, "Start recording error", e)
        }
    }

    fun stopRecording() {
        try {
            if (activity.isRecording && ::speechRecognizer.isInitialized) {
                speechRecognizer.stopListening()
                android.util.Log.d(TAG, "Recording stopped")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Stop recording error", e)
        } finally {
            activity.isRecording = false
            activity.btnRecord.setImageResource(R.drawable.ic_mic)
            activity.tvStatus.text = "Остановлено"
        }
    }

    fun resetSpeechRecognizer() {
        try {
            if (::speechRecognizer.isInitialized) {
                speechRecognizer.destroy()
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
                setRecognitionListener(CustomRecognitionListener(activity))
            }
            android.util.Log.d(TAG, "SpeechRecognizer reset")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error resetting SpeechRecognizer", e)
        }
    }

    fun destroy() {
        try {
            if (::speechRecognizer.isInitialized) {
                speechRecognizer.destroy()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "onDestroy error", e)
        }
    }
}