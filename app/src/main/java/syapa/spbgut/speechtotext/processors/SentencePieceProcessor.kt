package syapa.spbgut.speechtotext.processors

class SentencePieceProcessor private constructor(private val native: SentencePieceNativeProcessor) {
    companion object {
        fun create(): SentencePieceProcessor {
            return SentencePieceProcessor(SentencePieceNativeProcessor().init())
        }
    }

    fun load(modelPath: String): Boolean {
        return native.loadModel(modelPath)
    }

    fun encode(text: String): IntArray {
        return native.encode(text)
    }

    fun decode(ids: IntArray): String {
        return native.decode(ids)
    }

    fun close() {
        native.release()
    }
}