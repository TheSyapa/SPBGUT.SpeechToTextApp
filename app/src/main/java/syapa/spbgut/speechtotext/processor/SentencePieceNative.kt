package syapa.spbgut.speechtotext.processor

class SentencePieceNative {
    private var nativeHandle: Long = 0

    init {
        System.loadLibrary("sentencepiece_jni")
    }

    // Создает нативный обработчик
    external fun initNative(): Long

    // Загружает модель (вызывается после initNative)
    external fun loadModelNative(handle: Long, modelPath: String): Boolean

    // Кодирует текст
    external fun encodeNative(handle: Long, text: String): IntArray

    // Декодирует ID
    external fun decodeNative(handle: Long, ids: IntArray): String

    // Освобождает ресурсы
    external fun releaseNative(handle: Long)

    fun init(): SentencePieceNative {
        nativeHandle = initNative()
        return this
    }

    fun loadModel(modelPath: String): Boolean {
        require(nativeHandle != 0L) { "Call init() first" }
        return loadModelNative(nativeHandle, modelPath)
    }

    fun encode(text: String): IntArray {
        require(nativeHandle != 0L) { "Call init() first" }
        return encodeNative(nativeHandle, text)
    }

    fun decode(ids: IntArray): String {
        require(nativeHandle != 0L) { "Call init() first" }
        return decodeNative(nativeHandle, ids)
    }

    fun release() {
        if (nativeHandle != 0L) {
            releaseNative(nativeHandle)
            nativeHandle = 0
        }
    }
}