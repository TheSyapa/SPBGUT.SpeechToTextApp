package syapa.spbgut.speechtotext.managers

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import syapa.spbgut.speechtotext.MainActivity

class PermissionManager(private val activity: MainActivity) {

    private val recordAudioRequestCode = 101

    fun checkPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                recordAudioRequestCode
            )
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == recordAudioRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity, "Разрешение на микрофон получено!", Toast.LENGTH_SHORT)
                    .show()
                (activity as MainActivity).speechRecognitionManager.resetSpeechRecognizer()
            } else {
                Toast.makeText(
                    activity,
                    "Разрешение на использование микрофона обязательно!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}