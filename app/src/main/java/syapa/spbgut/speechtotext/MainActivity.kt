package syapa.spbgut.speechtotext

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var etResult: EditText
    private lateinit var tvStatus: TextView
    private lateinit var btnRecord: FloatingActionButton
    private lateinit var btnCopy: FloatingActionButton
    private lateinit var btnMenu: ImageButton
    private lateinit var createDocumentLauncher: ActivityResultLauncher<String>

    private var isRecording = false
    private val recordAudioRequestCode = 101
    private val TAG = "SpeechToTextApp"

    // Для сохранения позиции курсора при редактировании
    private var lastCursorPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация UI
        etResult = findViewById(R.id.etResult)
        tvStatus = findViewById(R.id.tvStatus)
        btnRecord = findViewById(R.id.btnRecord)
        btnCopy = findViewById(R.id.btnCopy)
        btnMenu = findViewById(R.id.btnMenu)

        // Инициализация лаунчера для создания документа
        createDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/plain")
        ) { uri ->
            if (uri != null) {
                saveTextToUri(uri)
            }
        }

        // Сохраняем позицию курсора при редактировании
        etResult.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                lastCursorPosition = etResult.selectionStart
            }
        })

        // Инициализация SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(createRecognitionListener())
        }

        // Настройка кнопок
        btnRecord.setOnClickListener { toggleRecording() }
        btnCopy.setOnClickListener { copyToClipboard() }
        btnMenu.setOnClickListener { showMenu(it) }
        setupButtonEffects()
        // Проверка разрешений
        checkPermission()
    }

    // Функция для показа меню
    private fun showMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_clear -> {
                    clearText()
                    true
                }

                R.id.menu_save -> {
                    saveToFile() // Прямой вызов без проверки разрешений
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    // Очистка текста
    private fun clearText() {
        etResult.setText("")
        Toast.makeText(this, "Текст очищен", Toast.LENGTH_SHORT).show()
    }

    // Сохранение в файл
    private fun saveToFile() {
        val text = etResult.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(this, "Текст пуст", Toast.LENGTH_SHORT).show()
            return
        }

        // Предлагаем имя файла по умолчанию
        val fileName = "voice_text_${System.currentTimeMillis()}.txt"
        createDocumentLauncher.launch(fileName)
    }

    private fun saveTextToUri(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write(etResult.text.toString())
                }
                Toast.makeText(this, "Файл сохранен", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения файла", e)
            Toast.makeText(
                this,
                "Ошибка сохранения: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                runOnUiThread {
                    tvStatus.text = "Говорите..."
                    Log.d(TAG, "onReadyForSpeech")
                }
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                runOnUiThread {
                    tvStatus.text = "Обработка..."
                    Log.d(TAG, "onEndOfSpeech")
                }
            }

            override fun onError(error: Int) {
                runOnUiThread {
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

                    tvStatus.text = "Ошибка: $errorMsg"
                    isRecording = false
                    btnRecord.setImageResource(R.drawable.ic_mic)
                    Log.e(TAG, "Recognition error: $errorMsg")

                    if (error == SpeechRecognizer.ERROR_CLIENT) {
                        resetSpeechRecognizer()
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                runOnUiThread {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.let { matches ->
                            if (matches.isNotEmpty()) {
                                val recognizedText = matches[0] + " "

                                // Вставляем распознанный текст в текущую позицию курсора
                                val currentText = etResult.text.toString()
                                val beforeCursor = currentText.substring(0, lastCursorPosition)
                                val afterCursor = currentText.substring(lastCursorPosition)

                                val newText = "$beforeCursor$recognizedText$afterCursor"
                                etResult.setText(newText)

                                // Восстанавливаем позицию курсора
                                val newCursorPosition = lastCursorPosition + recognizedText.length
                                etResult.setSelection(newCursorPosition)
                            }
                        }
                    isRecording = false
                    btnRecord.setImageResource(R.drawable.ic_mic)
                    tvStatus.text = "Готово. Нажмите кнопку для новой записи"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun resetSpeechRecognizer() {
        try {
            if (::speechRecognizer.isInitialized) {
                speechRecognizer.destroy()
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(createRecognitionListener())
            }
            Log.d(TAG, "SpeechRecognizer reset")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting SpeechRecognizer", e)
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        lastCursorPosition = etResult.selectionStart

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Распознавание речи недоступно", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")

                // Дополнительные параметры для улучшения качества
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите сейчас...")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5) // Больше вариантов

                // Предпочтение офлайн-режима
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            }

            speechRecognizer.startListening(intent)
            isRecording = true
            btnRecord.setImageResource(R.drawable.ic_stop)
            tvStatus.text = "Слушаем..."
            Log.d(TAG, "Recording started")
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Start recording error", e)
        }
    }

    private fun stopRecording() {
        try {
            if (isRecording && ::speechRecognizer.isInitialized) {
                speechRecognizer.stopListening()
                Log.d(TAG, "Recording stopped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stop recording error", e)
        } finally {
            isRecording = false
            btnRecord.setImageResource(R.drawable.ic_mic)
            tvStatus.text = "Остановлено"
        }
    }

    private fun copyToClipboard() {
        val textToCopy = etResult.text.toString()
        if (textToCopy.isNotEmpty()) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Распознанный текст", textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Текст скопирован в буфер", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Нет текста для копирования", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                recordAudioRequestCode
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupButtonEffects() {
        val scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down)
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up)

        btnRecord.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.startAnimation(scaleDown)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.startAnimation(scaleUp)
            }
            false
        }

        btnCopy.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.startAnimation(scaleDown)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.startAnimation(scaleUp)
            }
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == recordAudioRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на микрофон получено!", Toast.LENGTH_SHORT).show()
                resetSpeechRecognizer()
            } else {
                Toast.makeText(
                    this,
                    "Разрешение на использование микрофона обязательно!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::speechRecognizer.isInitialized) {
                speechRecognizer.destroy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy error", e)
        }
    }

}