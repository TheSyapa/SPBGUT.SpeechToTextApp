/*
package syapa.spbgut.speechtotext.processor

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import syapa.spbgut.speechtotext.punctuator.OnnxPunctuator
import java.io.File
import java.io.FileOutputStream

class EnhancedSentencePieceProcessor {
    private lateinit var nativeProcessor: SentencePieceNative
    private lateinit var punctuator: OnnxPunctuator

    fun initialize(context: Context, spModelName: String, onnxModelName: String): Boolean {
        try {
            // 1. Инициализация SentencePiece
            nativeProcessor = SentencePieceNative().init()
            val spModelPath = copyAssetToStorage(context, spModelName)
            if (!nativeProcessor.loadModel(spModelPath)) {
                throw IllegalStateException("Не удалось загрузить SentencePiece модель")
            }

            // 2. Инициализация модели пунктуации
            punctuator = OnnxPunctuator(context, onnxModelName)

            return true
        } catch (e: Exception) {
            Log.e("EnhancedSP", "Ошибка инициализации", e)
            return false
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun processWithPunctuation(text: String): ProcessResult {
        return try {
            // 1. Кодируем текст
            val originalIds = nativeProcessor.encode(text)

            // 2. Восстанавливаем пунктуацию с помощью ONNX
            val punctuatedIds = punctuator.restorePunctuation(originalIds)

            // 3. Декодируем обратно
            val decodedText = nativeProcessor.decode(punctuatedIds)

            ProcessResult(
                originalText = text,
                originalIds = originalIds,
                punctuatedIds = punctuatedIds,
                restoredText = decodedText
            )
        } catch (e: Exception) {
            Log.e("EnhancedSP", "Ошибка обработки", e)
            ProcessResult(error = e.message ?: "Unknown error")
        }
    }

    data class ProcessResult(
        val originalText: String = "",
        val originalIds: IntArray = intArrayOf(),
        val punctuatedIds: IntArray = intArrayOf(),
        val restoredText: String = "",
        val error: String? = null
    )

    private fun copyAssetToStorage(context: Context, fileName: String): String {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            context.assets.open(fileName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file.absolutePath
    }

    fun close() {
        nativeProcessor.release()
        punctuator.close()
    }
}*/
