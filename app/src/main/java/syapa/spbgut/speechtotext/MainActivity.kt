package syapa.spbgut.speechtotext

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import syapa.spbgut.speechtotext.enums.LogMessages
import syapa.spbgut.speechtotext.loggers.Logger
import syapa.spbgut.speechtotext.managers.ClipboardManager
import syapa.spbgut.speechtotext.managers.FileManager
import syapa.spbgut.speechtotext.managers.PermissionManager
import syapa.spbgut.speechtotext.managers.PunctuatorManager
import syapa.spbgut.speechtotext.managers.SpeechRecognitionManager
import syapa.spbgut.speechtotext.processors.SentencePieceProcessor
import syapa.spbgut.speechtotext.punctuators.OnnxPunctuator
import syapa.spbgut.speechtotext.ui.ButtonEffectsManager
import syapa.spbgut.speechtotext.ui.MenuManager
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"
    private val sentencePieceModelPath = "sp.model"
    private val onnxModelPath = "model.onnx"
    val showToastsInProduction = true
    var isRecording = false
    var lastCursorPosition = 0

    lateinit var logger: Logger
    lateinit var speechRecognitionManager: SpeechRecognitionManager
    lateinit var fileManager: FileManager
    lateinit var clipboardManager: ClipboardManager
    lateinit var permissionManager: PermissionManager
    lateinit var buttonEffectsManager: ButtonEffectsManager
    lateinit var menuManager: MenuManager
    lateinit var sentencePieceProcessor: SentencePieceProcessor
    lateinit var onnxPunctuator: OnnxPunctuator
    lateinit var punctuatorManager: PunctuatorManager

    lateinit var etResult: EditText
    lateinit var tvStatus: TextView
    lateinit var btnRecord: FloatingActionButton
    lateinit var btnCopy: FloatingActionButton
    lateinit var btnMenu: android.widget.ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        logger = Logger(tag, this, showToastsInProduction)
        try {
            initViews()
            initManagers()
            loadModels()
        } catch (e: Exception) {
            logger.handleError(LogMessages.INITIALIZATION_ERROR, e)
        }
        logger.logInfo(LogMessages.ACTIVITY_CREATED)
    }

    fun processTextWithModel() {
        try {
            punctuatorManager.processTextWithModel(etResult)
        } catch (e: Exception) {
            logger.handleError(LogMessages.TEXT_PROCESSING_ERROR, e)
        }
    }

    private fun copyModelFromAssets(modelName: String): String {
        val file = File(filesDir, modelName)
        if (!file.exists()) {
            try {
                assets.open(modelName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                logger.logInfo(LogMessages.MODEL_COPY_SUCCESS, modelName)
            } catch (e: Exception) {
                logger.logError(LogMessages.MODEL_COPY_ERROR, e, modelName)
                throw e
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

    private fun initManagers() {
        logger.logInfo(LogMessages.MANAGERS_INITIALIZATION)

        speechRecognitionManager = SpeechRecognitionManager(this, logger)
        fileManager = FileManager(this, logger)
        fileManager.initialize()
        clipboardManager = ClipboardManager(this, logger)
        permissionManager = PermissionManager(this)
        buttonEffectsManager = ButtonEffectsManager(this)
        menuManager = MenuManager(this)
        punctuatorManager = PunctuatorManager(this)

        setupButtonListeners()
        permissionManager.checkPermission()
    }

    private fun setupButtonListeners() {
        btnRecord.setOnClickListener { toggleRecording() }
        btnCopy.setOnClickListener { copyToClipboard() }
        btnMenu.setOnClickListener { showMenu(it) }
        buttonEffectsManager.setupButtonEffects()
    }

    private fun loadModels() {
        logger.logInfo(LogMessages.MODELS_LOADING_START)

        try {
            sentencePieceProcessor = SentencePieceProcessor.create()
            sentencePieceProcessor.load(copyModelFromAssets(sentencePieceModelPath))

            onnxPunctuator = OnnxPunctuator.create(logger)
            onnxPunctuator.loadModel(this, onnxModelPath)

            logger.logInfo(LogMessages.MODELS_LOADING_SUCCESS)
        } catch (e: Exception) {
            logger.handleError(LogMessages.MODELS_LOADING_CRITICAL_ERROR, e)
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
        try {
            speechRecognitionManager.startRecording()
            isRecording = true
        } catch (e: Exception) {
            logger.handleError(LogMessages.RECORDING_START_ERROR, e)
        }
    }

    private fun stopRecording() {
        try {
            speechRecognitionManager.stopRecording()
            isRecording = false
        } catch (e: Exception) {
            logger.logError(LogMessages.RECORDING_STOP_ERROR, e, showToast = false)
        }
    }

    private fun copyToClipboard() {
        try {
            clipboardManager.copyToClipboard(etResult.text.toString())
        } catch (e: Exception) {
            logger.handleError(LogMessages.COPY_TO_CLIPBOARD_ERROR, e)
        }
    }

    private fun showMenu(anchor: android.view.View) {
        try {
            menuManager.showMenu(anchor)
        } catch (e: Exception) {
            logger.logError(LogMessages.MENU_ERROR, e, showToast = false)
        }
    }

    fun clearText() {
        logger.logInfo(LogMessages.CLEAR_TEXT)
        etResult.setText("")
    }

    fun saveToFile() {
        try {
            fileManager.saveToFile(etResult.text.toString())
        } catch (e: Exception) {
            logger.handleError(LogMessages.SAVE_TO_FILE_ERROR, e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        logger.logInfo(LogMessages.PERMISSION_RESULT, requestCode)
        permissionManager.onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.logInfo(LogMessages.ACTIVITY_DESTROYED)
        try {
            speechRecognitionManager.destroy()
        } catch (e: Exception) {
            logger.logError(LogMessages.DESTROY_ERROR, e, showToast = false)
        }
    }
}