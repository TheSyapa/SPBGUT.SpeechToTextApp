package syapa.spbgut.speechtotext

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import syapa.spbgut.speechtotext.listeners.TextChangeListener
import syapa.spbgut.speechtotext.managers.ClipboardManager
import syapa.spbgut.speechtotext.managers.FileManager
import syapa.spbgut.speechtotext.managers.PermissionManager
import syapa.spbgut.speechtotext.managers.SpeechRecognitionManager
//import syapa.spbgut.speechtotext.processor.EnhancedSentencePieceProcessor
import syapa.spbgut.speechtotext.processor.SentencePiece
import syapa.spbgut.speechtotext.punctuator.OnnxPunctuator
import syapa.spbgut.speechtotext.ui.ButtonEffectsManager
import syapa.spbgut.speechtotext.ui.MenuManager
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    lateinit var speechRecognitionManager: SpeechRecognitionManager
    private lateinit var fileManager: FileManager
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var buttonEffectsManager: ButtonEffectsManager
    private lateinit var menuManager: MenuManager
    private lateinit var sentencePiece: SentencePiece
    private lateinit var onnxPunctuator: OnnxPunctuator

    // private lateinit var enhancedSentencePieceProcessor: EnhancedSentencePieceProcessor
    lateinit var etResult: EditText
    lateinit var tvStatus: TextView
    lateinit var btnRecord: FloatingActionButton
    lateinit var btnCopy: FloatingActionButton
    lateinit var btnMenu: android.widget.ImageButton

    var isRecording = false
    var lastCursorPosition = 0

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Создание и инициализация
        sentencePiece = SentencePiece.create()

        // Копируем модель из assets
        val modelPath = copyModelFromAssets("sp.model")

        // Загружаем модель
        if (sentencePiece.load(modelPath)) {
            // Используем
            val ids = sentencePiece.encode("Hello world")
            val text = sentencePiece.decode(ids)
            Log.d("SentencePiece", "IDs: ${ids.joinToString()}")
            Log.d("SentencePiece", "Text: $text")
        }

        onnxPunctuator = OnnxPunctuator.create()
        if (!onnxPunctuator.loadModel(this, "model.onnx")) {
            Log.e("MainActivity", "Не удалось загрузить ONNX модель")
        }

        initViews()
        initManagers()
        setupTextWatcher()
    }

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.N)
    fun processTextWithModel() {
        // Получаем текст из EditText
        val inputText = etResult.text.toString().trim()
        try {
            // 1. Кодируем текст с помощью SentencePiece
            val ids = sentencePiece.encode(inputText)
            Log.d("SentencePiece", "Закодированные ID (${ids.size}): ${ids.joinToString(", ")}")

            // 2. Добавляем BOS и EOS токены (ВАЖНО!)
            val inputIds = intArrayOf(1) + ids + intArrayOf(2)
            Log.d("SentencePiece", "ID с BOS/EOS (${inputIds.size}): ${inputIds.joinToString(", ")}")

            // 3. Восстанавливаем пунктуацию с помощью ONNX модели
            var punctuatedIds = ids
            if (onnxPunctuator.isLoaded()) {
                punctuatedIds = onnxPunctuator.restorePunctuation(inputIds, sentencePiece)
            } else {
                Log.e("MainActivity", "OnnxPunctuator не загружен")
            }
            Log.d("OnnxModel", "ID с пунктуацией (${punctuatedIds.size}): ${punctuatedIds.joinToString(", ")}")

            // 4. Декодируем обратно в текст
            val decodedText = sentencePiece.decode(punctuatedIds)

            // 5. Декодируем обратно в текст текст без обработки (ДЕБАГ)
            val decodedNOAIText = sentencePiece.decode(ids)

            // 6. Формируем результат
            val result = buildString {
                appendLine("=== ИСХОДНЫЙ ТЕКСТ ===")
                appendLine(inputText)
                appendLine()
                appendLine("=== ЗАКОДИРОВАННЫЕ ID (${ids.size}) ===")
                appendLine(ids.take(50).joinToString(", "))
                if (ids.size > 50) appendLine("... (еще ${ids.size - 50} ID)")
                appendLine()
                appendLine("=== ID С BOS/EOS (${inputIds.size}) ===")
                appendLine(inputIds.take(50).joinToString(", "))
                if (inputIds.size > 50) appendLine("... (еще ${inputIds.size - 50} ID)")
                appendLine()
                appendLine("=== ID С ПУНКТУАЦИЕЙ (${punctuatedIds.size}) ===")
                appendLine(punctuatedIds.take(50).joinToString(", "))
                if (punctuatedIds.size > 50) appendLine("... (еще ${punctuatedIds.size - 50} ID)")
                appendLine()
                appendLine("=== ДЕКОДИРОВАННЫЙ ТЕКСТ (БЕЗ ИИ) ===")
                appendLine(decodedNOAIText)
                appendLine("=== ДЕКОДИРОВАННЫЙ ТЕКСТ (С ИИ) ===")
                appendLine(decodedText)
            }

            // 7. Отображаем результат
            etResult.setText(result)
            etResult.setSelection(0)

        } catch (e: Exception) {
            etResult.error = "Ошибка: ${e.message}"
            Log.e("SentencePiece", "Ошибка обработки текста", e)
        }
    }
    private fun copyModelFromAssets(modelName: String): String {
        val file = File(filesDir, modelName)
        if (!file.exists()) {
            assets.open(modelName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file.absolutePath
    }

    private fun initViews() {
        etResult = findViewById(R.id.etResult)
        tvStatus = findViewById(R.id.tvStatus)
        btnRecord = findViewById(R.id.btnRecord)
        btnCopy = findViewById(R.id.btnCopy)
        btnMenu = findViewById(R.id.btnMenu)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun initManagers() {
        speechRecognitionManager = SpeechRecognitionManager(this)
        fileManager = FileManager(this)
        clipboardManager = ClipboardManager(this)
        permissionManager = PermissionManager(this)
        buttonEffectsManager = ButtonEffectsManager(this)
        menuManager = MenuManager(this)

        setupButtonListeners()
        permissionManager.checkPermission()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupButtonListeners() {
        btnRecord.setOnClickListener { toggleRecording() }
        btnCopy.setOnClickListener { copyToClipboard() }
        btnMenu.setOnClickListener { showMenu(it) }
        buttonEffectsManager.setupButtonEffects()
    }

    private fun setupTextWatcher() {
        etResult.addTextChangedListener(TextChangeListener(this))
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
        speechRecognitionManager.startRecording()
    }

    private fun stopRecording() {
        speechRecognitionManager.stopRecording()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun copyToClipboard() {
        clipboardManager.copyToClipboard(etResult.text.toString())
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun showMenu(anchor: android.view.View) {
        menuManager.showMenu(anchor)
    }

    fun clearText() {
        etResult.setText("")
    }

    fun saveToFile() {
        fileManager.saveToFile(etResult.text.toString())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognitionManager.destroy()
    }
}