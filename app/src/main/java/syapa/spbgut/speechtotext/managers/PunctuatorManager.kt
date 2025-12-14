package syapa.spbgut.speechtotext.managers

import android.widget.EditText
import syapa.spbgut.speechtotext.MainActivity

class PunctuatorManager(private val activity: MainActivity) {

    fun processTextWithModel(etResult: EditText) {
        val inputText = etResult.text.toString().trim()
        try {
            val ids = activity.sentencePieceProcessor.encode(inputText)
            val inputIds = intArrayOf(1) + ids + intArrayOf(2)

            var punctuatedIds = ids
            if (activity.onnxPunctuator.isLoaded()) {
                punctuatedIds =
                    activity.onnxPunctuator.restorePunctuation(
                        inputIds,
                        activity.sentencePieceProcessor
                    )
            }

            val decodedText = activity.sentencePieceProcessor.decode(punctuatedIds)
            val decodedNOAIText = activity.sentencePieceProcessor.decode(ids)

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

            etResult.setText(result)
            etResult.setSelection(0)

        } catch (e: Exception) {
            etResult.error = "Ошибка: ${e.message}"
        }
    }
}