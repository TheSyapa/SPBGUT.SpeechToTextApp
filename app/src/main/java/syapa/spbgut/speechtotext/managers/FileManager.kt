package syapa.spbgut.speechtotext.managers

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import syapa.spbgut.speechtotext.MainActivity
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class FileManager(private val activity: MainActivity) {

    private lateinit var createDocumentLauncher: ActivityResultLauncher<String>

    fun initLauncher(launcher: ActivityResultLauncher<String>) {
        createDocumentLauncher = launcher
    }

    fun saveToFile(text: String) {
        if (text.isEmpty()) {
            Toast.makeText(activity, "Текст пуст", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "voice_text_${System.currentTimeMillis()}.txt"
        createDocumentLauncher.launch(fileName)
    }

    fun saveTextToUri(uri: Uri, text: String) {
        try {
            activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write(text)
                }
                Toast.makeText(activity, "Файл сохранен", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("FileManager", "Ошибка сохранения файла", e)
            Toast.makeText(
                activity,
                "Ошибка сохранения: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}