package syapa.spbgut.speechtotext.processors

import syapa.spbgut.speechtotext.enums.LogMessages
import syapa.spbgut.speechtotext.loggers.Logger

class SentencePieceNativeProcessor(
    private val logger: Logger? = null
) {
    private var nativeHandle: Long = 0

    init {
        System.loadLibrary("sentencepiece_jni")
        logger?.logDebug(
            LogMessages.MODELS_LOADING_START, "Загрузка нативной библиотеки SentencePiece"
        )
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

    fun init(): SentencePieceNativeProcessor {
        logger?.logDebug(LogMessages.MODELS_LOADING_START, "Инициализация нативного обработчика")
        nativeHandle = initNative()
        logger?.logDebug(LogMessages.MODELS_LOADING_SUCCESS, "Нативный обработчик инициализирован")
        return this
    }

    fun loadModel(modelPath: String): Boolean {
        require(nativeHandle != 0L) { "Call init() first" }

        logger?.logInfo(
            LogMessages.MODELS_LOADING_START,
            "Загрузка модели SentencePiece: ${modelPath.substringAfterLast("/")}"
        )

        val result = loadModelNative(nativeHandle, modelPath)

        if (result) {
            logger?.logInfo(LogMessages.MODELS_LOADING_SUCCESS, "Модель SentencePiece загружена")
        } else {
            logger?.logError(
                LogMessages.MODELS_LOADING_CRITICAL_ERROR
            )
        }

        return result
    }

    fun encode(text: String): IntArray {
        require(nativeHandle != 0L) { "Call init() first" }

        logger?.logDebug(
            LogMessages.TEXT_PROCESSING_ERROR, "Кодирование текста, длина: ${text.length}"
        )

        return encodeNative(nativeHandle, text)
    }

    fun decode(ids: IntArray): String {
        require(nativeHandle != 0L) { "Call init() first" }

        logger?.logDebug(
            LogMessages.TEXT_PROCESSING_ERROR, "Декодирование массива, размер: ${ids.size}"
        )

        return decodeNative(nativeHandle, ids)
    }

    fun release() {
        if (nativeHandle != 0L) {
            logger?.logDebug(LogMessages.DESTROY_ERROR, "Освобождение ресурсов SentencePiece")
            releaseNative(nativeHandle)
            nativeHandle = 0
            logger?.logDebug(LogMessages.DESTROY_ERROR, "Ресурсы SentencePiece освобождены")
        }
    }
}