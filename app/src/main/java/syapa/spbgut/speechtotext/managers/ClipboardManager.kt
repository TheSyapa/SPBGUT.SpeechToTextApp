package syapa.spbgut.speechtotext.managers

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import syapa.spbgut.speechtotext.MainActivity
import android.content.ClipboardManager as SystemClipboardManager

class ClipboardManager(private val activity: MainActivity) {

    @RequiresApi(Build.VERSION_CODES.M)
    fun copyToClipboard(textToCopy: String) {
        if (textToCopy.isNotEmpty()) {
            val clipboard = activity.getSystemService(SystemClipboardManager::class.java)
            val clip = ClipData.newPlainText("Распознанный текст", textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(activity, "Текст скопирован в буфер", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, "Нет текста для копирования", Toast.LENGTH_SHORT).show()
        }
    }
}