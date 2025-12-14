package syapa.spbgut.speechtotext.punctuators

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import syapa.spbgut.speechtotext.enums.LogMessages
import syapa.spbgut.speechtotext.loggers.Logger
import syapa.spbgut.speechtotext.processors.SentencePieceProcessor
import java.io.File
import java.io.FileOutputStream
import java.nio.LongBuffer

class OnnxPunctuator private constructor(
    private val logger: Logger
) {
    companion object {
        fun create(logger: Logger): OnnxPunctuator {
            return OnnxPunctuator(logger)
        }
    }

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false

    // Метки для предсказаний
    private val preLabels = arrayOf(
        "<NULL>",
        "¿",
        "¡",
        "\"",
        "(",
        "[",
        "{",
        "«",
        "„"
    )

    private val postLabels = arrayOf(
        "<NULL>",
        "<ACRONYM>",
        ".",
        ",",
        "?",
        "!",
        ":",
        ";",
        "…",
        "—",
        "-",
        "–",
        ")",
        "]",
        "}",
        "\"",
        "»",
        "“",
        "？",
        "，",
        "。",
        "、",
        "・",
        "।",
        "؟",
        "،",
        "።",
        "፣",
        "፧"
    )

    private val nullToken = ""
    private val bosId = 1
    private val eosId = 2

    fun loadModel(context: Context, modelName: String = "model.onnx"): Boolean {
        try {
            // Копируем модель из assets
            val modelFile = File(context.filesDir, modelName)
            if (!modelFile.exists()) {
                context.assets.open(modelName).use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                        logger.logInfo(LogMessages.MODEL_COPY_SUCCESS, modelName)
                    }
                }
            }

            // Инициализируем ONNX Runtime
            ortEnv = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions()

            ortSession = ortEnv?.createSession(modelFile.absolutePath, sessionOptions)
            isInitialized = true

            logger.logInfo(LogMessages.ONNX_MODEL_LOADED)

            return true
        } catch (e: Exception) {
            logger.logError(LogMessages.ONNX_LOAD_ERROR, e)
            return false
        }
    }

    private fun logPredictions(
        inputIds: IntArray,
        prePreds: LongArray,
        postPreds: LongArray,
        sentencePieceProcessor: SentencePieceProcessor
    ) {
        logger.logDebug(LogMessages.ONNX_PREDICTIONS_HEADER)

        val predictionLogs = mutableListOf<String>()

        for (i in inputIds.indices) {
            val tokenId = inputIds[i]
            if (tokenId == bosId || tokenId == eosId) continue

            val tokenText = try {
                val ids = intArrayOf(tokenId)
                sentencePieceProcessor.decode(ids)
            } catch (e: Exception) {
                "???"
            }

            val preIdx = if (i < prePreds.size) prePreds[i].toInt() else 0
            val postIdx = if (i < postPreds.size) postPreds[i].toInt() else 0

            val prePunct = if (preIdx > 0 && preIdx < preLabels.size) preLabels[preIdx] else "NULL"
            val postPunct = if (postIdx > 0 && postIdx < postLabels.size) postLabels[postIdx] else "NULL"

            if (preIdx > 0 || postIdx > 0) {
                predictionLogs.add(
                    "Токен $i: '$tokenText' (ID: $tokenId) -> " +
                            "Pre: $prePunct ($preIdx), Post: $postPunct ($postIdx)"
                )
            }
        }

        // Выводим все предсказания одной строкой
        if (predictionLogs.isNotEmpty()) {
            logger.logDebug(LogMessages.ONNX_PREDICTIONS_DETAILS, predictionLogs.joinToString("; "))
        } else {
            logger.logDebug(LogMessages.ONNX_NO_PREDICTIONS)
        }

        logger.logDebug(LogMessages.ONNX_PREDICTIONS_FOOTER)
    }

    fun restorePunctuation(
        inputIds: IntArray,
        sentencePieceProcessor: SentencePieceProcessor
    ): IntArray {
        if (!isInitialized || ortSession == null) {
            logger.logError(LogMessages.ONNX_MODEL_NOT_LOADED)
            return inputIds
        }

        try {
            if (inputIds.isEmpty() || inputIds[0] != bosId || inputIds.last() != eosId) {
                logger.logWarning(
                    LogMessages.ONNX_INVALID_INPUT
                )
            }

            // 1. Преобразуем входные данные в формат модели
            val tensorInput = prepareInputTensor(inputIds)

            // 2. Запускаем инференс
            val results = ortSession?.run(tensorInput)

            if (results == null) {
                logger.logError(LogMessages.ONNX_INFERENCE_FAILED)
                return inputIds
            }

            logger.logDebug(LogMessages.ONNX_RESULTS_RECEIVED, results.size())

            // 3. Получаем все 4 выхода
            val prePreds = results["pre_preds"]?.get()?.value as? Array<LongArray>
            prePreds?.get(0)?.let { array ->
                val prePredsStr = array.joinToString(", ")
                logger.logDebug(LogMessages.ONNX_PRE_PREDS_ARRAY, prePredsStr)
            }

            val postPreds = results["post_preds"]?.get()?.value as? Array<LongArray>
            postPreds?.get(0)?.let { array ->
                val postPredsStr = array.joinToString(", ")
                logger.logDebug(LogMessages.ONNX_POST_PREDS_ARRAY, postPredsStr)
            }

            val capPreds = results["cap_preds"]?.get()?.value as? Array<LongArray>
            capPreds?.get(0)?.let { array ->
                val capPredsStr = array.joinToString(", ")
                logger.logDebug(LogMessages.ONNX_CAP_PREDS_ARRAY, capPredsStr)
            }

            val segPreds = results["seg_preds"]?.get()?.value as? Array<LongArray>
            segPreds?.get(0)?.let { array ->
                val segPredsStr = array.joinToString(", ")
                logger.logDebug(LogMessages.ONNX_SEG_PREDS_ARRAY, segPredsStr)
            }

            if (prePreds == null || postPreds == null) {
                logger.logError(LogMessages.ONNX_PREDICTIONS_MISSING)
                return inputIds
            }

            logger.logDebug(LogMessages.ONNX_INPUT_SIZE, inputIds.size)
            logger.logDebug(LogMessages.ONNX_PRE_SIZE, prePreds[0].size)
            logger.logDebug(LogMessages.ONNX_POST_SIZE, postPreds[0].size)

            // Проверяем соответствие размеров
            if (inputIds.size != prePreds[0].size) {
                logger.logWarning(
                    LogMessages.ONNX_SIZE_MISMATCH
                )
            }

            logPredictions(inputIds, prePreds[0], postPreds[0], sentencePieceProcessor)

            // 4. Обрабатываем результаты
            return processPredictions(
                inputIds, prePreds[0], postPreds[0],
                capPreds?.get(0), segPreds?.get(0),
                sentencePieceProcessor
            )

        } catch (e: Exception) {
            logger.logError(LogMessages.ONNX_INFERENCE_ERROR, e)
            return inputIds
        }
    }

    private fun prepareInputTensor(inputIds: IntArray): Map<String, OnnxTensor> {
        // Преобразуем IntArray в long[] для ONNX
        val longIds = inputIds.map { it.toLong() }.toLongArray()
        val shape = longArrayOf(1, longIds.size.toLong())

        val inputTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(longIds), shape)
        return mapOf("input_ids" to inputTensor)
    }

    private fun processPredictions(
        inputIds: IntArray,
        prePreds: LongArray,
        postPreds: LongArray,
        capPreds: LongArray?,
        segPreds: LongArray?,
        sentencePieceProcessor: SentencePieceProcessor
    ): IntArray {
        val result = mutableListOf<Int>()
        val addedPunctuations = mutableListOf<String>() // Собираем добавленные знаки препинания

        logger.logDebug(LogMessages.ONNX_PROCESSING_START, inputIds.size)

        // Собираем токены с их пунктуацией
        for (i in inputIds.indices) {
            val tokenId = inputIds[i]

            // Пропускаем BOS и EOS токены
            if (tokenId == bosId || tokenId == eosId) {
                continue
            }

            // Проверяем границы массивов
            if (i >= prePreds.size || i >= postPreds.size) {
                result.add(tokenId)
                continue
            }

            // Получаем предсказания пунктуации
            val preIdx = prePreds[i].toInt()
            val postIdx = postPreds[i].toInt()

            // Добавляем пре-пунктуацию
            if (preIdx > 0 && preIdx < preLabels.size) {
                val prePunct = preLabels[preIdx]
                if (prePunct != nullToken && prePunct != "NULL" && prePunct != "<NULL>") {
                    if (result.isNotEmpty()) {
                        val lastToken = result.last()
                        try {
                            val lastTokenText = sentencePieceProcessor.decode(intArrayOf(lastToken))
                            if (lastTokenText !in punctuationMarks) {
                                addPunctuation(
                                    prePunct,
                                    result,
                                    sentencePieceProcessor,
                                    addedPunctuations
                                )
                            }
                        } catch (e: Exception) {
                            addPunctuation(
                                prePunct,
                                result,
                                sentencePieceProcessor,
                                addedPunctuations
                            )
                        }
                    } else {
                        addPunctuation(prePunct, result, sentencePieceProcessor, addedPunctuations)
                    }
                }
            }

            // Добавляем текущий токен
            result.add(tokenId)

            // Добавляем пост-пунктуацию
            if (postIdx > 0 && postIdx < postLabels.size) {
                val postPunct = postLabels[postIdx]
                if (postPunct != nullToken && postPunct != "NULL" && postPunct != "<NULL>") {
                    addPunctuation(postPunct, result, sentencePieceProcessor, addedPunctuations)
                }
            }
        }

        // Выводим все добавленные знаки препинания одной строкой
        if (addedPunctuations.isNotEmpty()) {
            logger.logDebug(
                LogMessages.ONNX_PUNCT_ADDED_MULTIPLE,
                addedPunctuations.joinToString(", ")
            )
        }

        logger.logDebug(LogMessages.ONNX_PROCESSING_END, result.size)

        return postProcessTokens(result, sentencePieceProcessor)
    }

    private fun addPunctuation(
        punctuation: String,
        result: MutableList<Int>,
        sentencePieceProcessor: SentencePieceProcessor,
        addedPunctuations: MutableList<String> // Добавляем параметр для сбора
    ) {
        try {
            val punctIds = sentencePieceProcessor.encode(punctuation)
            if (punctIds.isNotEmpty()) {
                result.addAll(punctIds.toList())
                addedPunctuations.add(punctuation) // Собираем знаки
            }
        } catch (e: Exception) {
            logger.logError(LogMessages.ONNX_ENCODE_ERROR, e)
        }
    }

    // Список знаков препинания для проверки
    private val punctuationMarks = setOf(
        ".", ",", "?", "!", ";", ":", "…", "—", "-", "–",
        ")", "]", "}", "\"", "»", "“", "？", "，", "。", "、",
        "・", "।", "؟", "،", "።", "፣", "፧"
    )

    private fun postProcessTokens(
        tokens: List<Int>,
        sentencePieceProcessor: SentencePieceProcessor
    ): IntArray {
        if (tokens.isEmpty()) return intArrayOf()

        // Декодируем токены в текст
        val text = try {
            sentencePieceProcessor.decode(tokens.toIntArray())
        } catch (e: Exception) {
            logger.logError(LogMessages.ONNX_DECODE_ERROR, e)
            return tokens.toIntArray()
        }

        // Убираем пробелы перед знаками препинания и исправляем двойные запятые
        var processedText = text

        // 1. Убираем пробелы перед знаками препинания
        val punctuationMarks = listOf(",", ".", "?", "!", ";", ":", "…", "—", "-", "–", ")", "]", "}")
        for (mark in punctuationMarks) {
            processedText = processedText.replace(" $mark", mark)
        }

        // 2. Убираем лишние пробелы после знаков препинания (кроме скобок и кавычек)
        val openPunctuation = listOf("(", "[", "{", "«", "„", "\"")
        for (mark in openPunctuation) {
            processedText = processedText.replace("$mark ", mark)
        }

        // 3. Убираем двойные и лишние запятые
        processedText = processedText.replace(",,", ",")
        processedText = processedText.replace(", ,", ",")
        processedText = processedText.replace(".,", ".")
        processedText = processedText.replace("?,", "?")
        processedText = processedText.replace("!,", "!")

        // 4. Добавляем пробел после запятой, точки с запятой, двоеточия, если его нет и если дальше идет буква
        processedText = addSpaceAfterComma(processedText)

        // 5. Убираем двойные пробелы
        processedText = processedText.replace("  ", " ")

        // 6. Исправляем регистр в начале предложений
        processedText = fixSentenceCapitalization(processedText)

        // Кодируем обратно
        return try {
            sentencePieceProcessor.encode(processedText)
        } catch (e: Exception) {
            logger.logError(LogMessages.ONNX_ENCODE_ERROR, e)
            tokens.toIntArray()
        }
    }

    private fun addSpaceAfterComma(text: String): String {
        var result = text
        val punctuation = listOf(",", ";", ":")

        for (mark in punctuation) {
            val regex = Regex("$mark([а-яА-Яa-zA-Z])")
            result = regex.replace(result) { matchResult ->
                "$mark ${matchResult.groupValues[1]}"
            }
        }

        return result
    }

    private fun fixSentenceCapitalization(text: String): String {
        if (text.isEmpty()) return text

        val result = StringBuilder()
        val sentences = text.split(Regex("(?<=[.!?…])\\s+"))

        for ((index, sentence) in sentences.withIndex()) {
            if (sentence.isNotEmpty()) {
                var processedSentence = sentence.trim()

                // Делаем первую букву заглавной, если она строчная
                if (processedSentence.isNotEmpty()) {
                    val firstChar = processedSentence[0]
                    if (firstChar.isLowerCase() && (firstChar in 'а'..'я' || firstChar in 'a'..'z')) {
                        processedSentence = firstChar.uppercaseChar() + processedSentence.substring(1)
                    }
                }

                if (index > 0) result.append(" ")
                result.append(processedSentence)
            }
        }

        return result.toString()
    }

    fun close() {
        ortSession?.close()
        ortEnv?.close()
        isInitialized = false
    }

    fun isLoaded(): Boolean {
        return isInitialized && ortSession != null
    }
}