package syapa.spbgut.speechtotext.processor

class SentencePiece private constructor(private val native: SentencePieceNative) {
    companion object {
        fun create(): SentencePiece {
            return SentencePiece(SentencePieceNative().init())
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