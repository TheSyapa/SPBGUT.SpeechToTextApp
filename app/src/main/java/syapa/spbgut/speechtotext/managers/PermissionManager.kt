package syapa.spbgut.speechtotext.managers

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import syapa.spbgut.speechtotext.MainActivity

class PermissionManager(private val activity: MainActivity) {

    private val recordAudioRequestCode = 101

    private companion object {
        const val TAG = "PermissionManager"
        const val SUB_TAG_PERMISSION = "RECORD_AUDIO"
    }

    fun checkPermission() {
        Log.d(TAG, "checkPermission() called - проверка разрешения на запись аудио")

        val permissionCheckResult = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        )

        when (permissionCheckResult) {
            PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Разрешение $SUB_TAG_PERMISSION уже предоставлено")
            }

            PackageManager.PERMISSION_DENIED -> {
                Log.w(TAG, "Разрешение $SUB_TAG_PERMISSION не предоставлено, запрашиваем...")

                // Проверяем, нужно ли показывать объяснение
                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                )

                Log.d(TAG, "Нужно ли показывать объяснение: $shouldShowRationale")

                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    recordAudioRequestCode
                )

                Log.i(TAG, "Запрос разрешения отправлен. Код запроса: $recordAudioRequestCode")
            }
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray
    ) {
        Log.d(
            TAG,
            "onRequestPermissionsResult: код запроса=$requestCode, результат=${grantResults.contentToString()}"
        )

        if (requestCode == recordAudioRequestCode) {
            when {
                grantResults.isEmpty() -> {
                    Log.e(TAG, "Ошибка: пустой массив результатов разрешения")
                    showPermissionDeniedToast()
                }

                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    Log.i(TAG, "Пользователь предоставил разрешение $SUB_TAG_PERMISSION")

                    Toast.makeText(
                        activity,
                        "Разрешение на микрофон получено!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    Log.w(TAG, "Пользователь отказал в разрешении $SUB_TAG_PERMISSION")

                    // Проверяем, отказано ли навсегда (пользователь выбрал "Не спрашивать снова")
                    val isPermanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.RECORD_AUDIO
                    )

                    if (isPermanentlyDenied) {
                        Log.e(TAG, "Разрешение отклонено навсегда (отмечено 'Не спрашивать снова')")
                        Toast.makeText(
                            activity,
                            "Разрешение отклонено. Включите его вручную в настройках приложения",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Log.d(TAG, "Разрешение отклонено, но можно запросить снова")
                        Toast.makeText(
                            activity,
                            "Разрешение на использование микрофона обязательно!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            Log.w(
                TAG,
                "Неизвестный код запроса разрешения: $requestCode (ожидался: $recordAudioRequestCode)"
            )
        }
    }

    private fun showPermissionDeniedToast() {
        Toast.makeText(
            activity,
            "Разрешение на использование микрофона обязательно!",
            Toast.LENGTH_LONG
        ).show()
    }
}