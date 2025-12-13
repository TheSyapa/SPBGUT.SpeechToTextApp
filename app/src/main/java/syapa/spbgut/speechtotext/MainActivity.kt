package syapa.spbgut.speechtotext

import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import syapa.spbgut.speechtotext.managers.SpeechRecognitionManager
import syapa.spbgut.speechtotext.managers.FileManager
import syapa.spbgut.speechtotext.managers.ClipboardManager
import syapa.spbgut.speechtotext.managers.PermissionManager
import syapa.spbgut.speechtotext.ui.ButtonEffectsManager
import syapa.spbgut.speechtotext.ui.MenuManager
import syapa.spbgut.speechtotext.listeners.TextChangeListener

class MainActivity : AppCompatActivity() {

    lateinit var speechRecognitionManager: SpeechRecognitionManager
    private lateinit var fileManager: FileManager
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var buttonEffectsManager: ButtonEffectsManager
    private lateinit var menuManager: MenuManager

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

        initViews()
        initManagers()
        setupTextWatcher()
    }

    private fun initViews() {
        etResult = findViewById(R.id.etResult)
        tvStatus = findViewById(R.id.tvStatus)
        btnRecord = findViewById(R.id.btnRecord)
        btnCopy = findViewById(R.id.btnCopy)
        btnMenu = findViewById(R.id.btnMenu)
    }

    @RequiresApi(Build.VERSION_CODES.M)
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

    @RequiresApi(Build.VERSION_CODES.M)
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