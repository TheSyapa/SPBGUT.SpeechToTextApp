package syapa.spbgut.speechtotext.punctuator

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import syapa.spbgut.speechtotext.processor.SentencePiece
import java.io.File
import java.io.FileOutputStream
import java.nio.LongBuffer

class OnnxPunctuator constructor() {
    companion object {
        fun create(): OnnxPunctuator {
            return OnnxPunctuator()
        }
    }

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false

    // Метки для предсказаний
    // Более полные массивы меток
    private val preLabels = arrayOf(
        "<NULL>",
        "¿",      // Испанский перевернутый вопросительный знак
        "¡",      // Испанский перевернутый восклицательный знак
        "\"",     // Кавычка
        "(",      // Открывающая скобка
        "[",      // Открывающая квадратная скобка
        "{",      // Открывающая фигурная скобка
        "«",      // Открывающая французская кавычка
        "„"       // Открывающая немецкая кавычка
    )

    private val postLabels = arrayOf(
        "<NULL>",
        "<ACRONYM>",
        ".",      // Точка
        ",",      // Запятая
        "?",      // Вопросительный знак
        "!",      // Восклицательный знак
        ":",      // Двоеточие
        ";",      // Точка с запятой
        "…",      // Многоточие
        "—",      // Тире
        "-",      // Дефис
        "–",      // Короткое тире
        ")",      // Закрывающая скобка
        "]",      // Закрывающая квадратная скобка
        "}",      // Закрывающая фигурная скобка
        "\"",     // Кавычка
        "»",      // Закрывающая французская кавычка
        "“",      // Закрывающая немецкая кавычка
        "？",     // Китайский вопросительный знак
        "，",     // Китайская запятая
        "。",     // Китайская точка
        "、",     // Китайский перечислительный знак
        "・",     // Японская точка
        "।",      // Деванагари точка
        "؟",      // Арабский вопросительный знак
        "،",      // Арабская запятая
        "።",      // Эфиопская точка
        "፣",      // Эфиопская запятая
        "፧"       // Эфиопский вопросительный знак
    )
    /*private val preLabels = arrayOf("<NULL>", "¿")
    private val postLabels = arrayOf(
        "<NULL>",
        "<ACRONYM>",
        ".",
        ",",
        "?",
        "？",
        "，",
        "。",
        "、",
        "・",
        "।",
        "؟",
        "،",
        ";",
        "።",
        "፣",
        "፧",
    )*/
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
                    }
                }
            }

            // Инициализируем ONNX Runtime
            ortEnv = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions()

            // Оптимизация для мобильных устройств
            // sessionOptions.interOpNumThreads = 1
            //  sessionOptions.intraOpNumThreads = 1

            ortSession = ortEnv?.createSession(modelFile.absolutePath, sessionOptions)
            isInitialized = true

            Log.d("OnnxPunctuator", "Модель загружена успешно")
            Log.d("OnnxPunctuator", "Входы: ${ortSession?.inputInfo?.keys?.joinToString()}")
            Log.d("OnnxPunctuator", "Выходы: ${ortSession?.outputInfo?.keys?.joinToString()}")

            return true
        } catch (e: Exception) {
            Log.e("OnnxPunctuator", "Ошибка загрузки модели", e)
            return false
        }
    }
    private fun logPredictions(
        inputIds: IntArray,
        prePreds: LongArray,
        postPreds: LongArray,
        sentencePiece: SentencePiece
    ) {
        Log.d("OnnxPunctuator", "=== ЛОГИРОВАНИЕ ПРЕДСКАЗАНИЙ ===")

        for (i in inputIds.indices) {
            val tokenId = inputIds[i]
            if (tokenId == 1 || tokenId == 2) continue

            val tokenText = try {
                val ids = intArrayOf(tokenId)
                sentencePiece.decode(ids)
            } catch (e: Exception) {
                "???"
            }

            val preIdx = if (i < prePreds.size) prePreds[i].toInt() else 0
            val postIdx = if (i < postPreds.size) postPreds[i].toInt() else 0

            val prePunct = if (preIdx > 0 && preIdx < preLabels.size) preLabels[preIdx] else "NULL"
            val postPunct = if (postIdx > 0 && postIdx < postLabels.size) postLabels[postIdx] else "NULL"

            if (preIdx > 0 || postIdx > 0) {
                Log.d("OnnxPunctuator",
                    "Токен $i: '$tokenText' (ID: $tokenId) -> " +
                            "Pre: $prePunct ($preIdx), Post: $postPunct ($postIdx)"
                )
            }
        }
        Log.d("OnnxPunctuator", "=== КОНЕЦ ЛОГИРОВАНИЯ ===")
    }
    @RequiresApi(Build.VERSION_CODES.N)
    fun restorePunctuation(inputIds: IntArray, sentencePiece: SentencePiece): IntArray {
        if (!isInitialized || ortSession == null) {
            Log.e("OnnxPunctuator", "Модель не загружена. Сначала вызовите loadModel()")
            return inputIds
        }

        try {
            if (inputIds.isEmpty() || inputIds[0] != 1 || inputIds.last() != 2) {
                Log.w(
                    "OnnxPunctuator",
                    "Входные данные не содержат BOS/EOS. Первый токен: ${inputIds.firstOrNull()}, последний: ${inputIds.lastOrNull()}"
                )
            }

            // 1. Преобразуем входные данные в формат модели
            val tensorInput = prepareInputTensor(inputIds)

            // 2. Запускаем инференс
            val results = ortSession?.run(tensorInput)

            if (results == null) {
                Log.e("OnnxPunctuator", "Не удалось получить результаты инференса")
                return inputIds
            }

            Log.d("OnnxPunctuator", "Результаты получены: ${results.size()}")

            // 3. Получаем все 4 выхода
            val prePreds = results["pre_preds"]?.get()?.value as? Array<LongArray>
            if (prePreds != null) {
                prePreds.forEach { item -> Log.d("OnnxPunctuator", "pre_preds: ${item}") }
            }
            val postPreds = results["post_preds"]?.get()?.value as? Array<LongArray>
            if (postPreds != null) {
                postPreds.forEach { item -> Log.d("OnnxPunctuator", "postPreds: ${item}") }
            }
            val capPreds = results["cap_preds"]?.get()?.value as? Array<LongArray>
            if (capPreds != null) {
                capPreds.forEach { item -> Log.d("OnnxPunctuator", "capPreds: ${item}") }
            }
            val segPreds = results["seg_preds"]?.get()?.value as? Array<LongArray>
            if (segPreds != null) {
                segPreds.forEach { item -> Log.d("OnnxPunctuator", "segPreds: ${item}") }
            }

            if (prePreds == null || postPreds == null) {
                Log.e("OnnxPunctuator", "Не удалось получить предсказания pre_preds или post_preds")
                return inputIds
            }

            Log.d("OnnxPunctuator", "Размер inputIds: ${inputIds.size}")
            Log.d("OnnxPunctuator", "Размер prePreds: ${prePreds[0].size}")
            Log.d("OnnxPunctuator", "Размер postPreds: ${postPreds[0].size}")

            // Проверяем соответствие размеров
            if (inputIds.size != prePreds[0].size) {
                Log.w(
                    "OnnxPunctuator",
                    "Размер inputIds (${inputIds.size}) не совпадает с размером prePreds (${prePreds[0].size})"
                )
            }

            logPredictions(inputIds, prePreds[0], postPreds[0], sentencePiece)
            // 4. Обрабатываем результаты
            return processPredictions(
                inputIds, prePreds[0], postPreds[0],
                capPreds?.get(0), segPreds?.get(0),
                sentencePiece
            )

        } catch (e: Exception) {
            Log.e("OnnxPunctuator", "Ошибка инференса", e)
            return inputIds
        }
    }

    private fun prepareInputTensor(inputIds: IntArray): Map<String, OnnxTensor> {
        // Преобразуем IntArray в long[] для ONNX
        val longIds = inputIds.map { it.toLong() }.toLongArray()
        val shape = longArrayOf(1, longIds.size.toLong()) // [batch_size, sequence_length]

        val inputTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(longIds), shape)
        return mapOf("input_ids" to inputTensor)
    }

    /* private fun processPredictions(
         inputIds: IntArray,
         prePreds: LongArray,
         postPreds: LongArray,
         capPreds: LongArray?,
         segPreds: LongArray?
     ): IntArray {
         val result = mutableListOf<Int>()

         // Обрабатываем каждый токен (игнорируем BOS/EOS как в Python коде)
         for (i in 1 until minOf(inputIds.size, prePreds.size) - 1) {
             val tokenId = inputIds[i]

             // Получаем предсказания для текущего токена
             val preIdx = prePreds[i].toInt()
             val postIdx = postPreds[i].toInt()

             // Добавляем предшествующую пунктуацию
             if (preIdx > 0 && preIdx < preLabels.size) {
                 val prePunct = preLabels[preIdx]
                 if (prePunct != nullToken) {
                     // Здесь нужно будет добавить логику преобразования
                     // знака пунктуации в ID через SentencePiece
                 }
             }

             // Добавляем текущий токен
             result.add(tokenId)

             // Добавляем последующую пунктуацию
             if (postIdx > 0 && postIdx < postLabels.size) {
                 val postPunct = postLabels[postIdx]
                 if (postPunct != nullToken) {
                     // Здесь будет добавление пунктуации
                 }
             }
         }

         return result.toIntArray()
     }*/
    /* private fun processPredictions(
         inputIds: IntArray,
         prePreds: LongArray,
         postPreds: LongArray,
         capPreds: LongArray?,
         segPreds: LongArray?,
         sentencePiece: SentencePiece
     ): IntArray {
         Log.d("OnnxPunctuator", "Начало обработки. inputIds.size = ${inputIds.size}")

         val result = mutableListOf<Int>()

         // Определяем границы обработки
         val startIdx = 1 // пропускаем BOS токен
         val endIdx = minOf(inputIds.size, prePreds.size) - 1 // останавливаемся перед EOS

         for (i in startIdx until endIdx) {
             val tokenId = inputIds[i]

             if (i >= prePreds.size || i >= postPreds.size) {
                 Log.w("OnnxPunctuator", "Индекс $i выходит за границы массивов предсказаний")
                 result.add(tokenId)
                 continue
             }

             // Получаем предсказания для текущего токена
             val preIdx = prePreds[i].toInt()
             val postIdx = postPreds[i].toInt()

             // 1. Добавляем предшествующую пунктуацию
             if (preIdx > 0 && preIdx < preLabels.size) {
                 val prePunct = preLabels[preIdx]
                 if (prePunct != nullToken) {
                     // Кодируем знак пунктуации через SentencePiece
                     try {
                         val punctIds = sentencePiece.encode(prePunct)
                         result.addAll(punctIds.toList())
                     } catch (e: Exception) {
                         Log.e("OnnxPunctuator", "Ошибка кодирования пунктуации: $prePunct", e)
                     }
                 }
             }

             // 2. Добавляем текущий токен
             result.add(tokenId)

             // 3. Добавляем последующую пунктуацию
             if (postIdx > 0 && postIdx < postLabels.size) {
                 val postPunct = postLabels[postIdx]
                 if (postPunct != nullToken) {
                     // Кодируем знак пунктуации через SentencePiece
                     try {
                         val punctIds = sentencePiece.encode(postPunct)
                         result.addAll(punctIds.toList())
                     } catch (e: Exception) {
                         Log.e("OnnxPunctuator", "Ошибка кодирования пунктуации: $postPunct", e)
                     }
                 }
             }

             // 4. Обрабатываем границы предложений (если есть)
             segPreds?.let {
                 if (i < it.size && it[i] > 0L && i < endIdx - 1) {
                     // Добавляем точку в конце предложения
                     try {
                         val dotIds = sentencePiece.encode(".")
                         result.addAll(dotIds.toList())
                     } catch (e: Exception) {
                         Log.e("OnnxPunctuator", "Ошибка добавления точки", e)
                     }
                 }
             }
         }

         return result.toIntArray()
     }*/
    /*@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun processPredictions(
        inputIds: IntArray,
        prePreds: LongArray,
        postPreds: LongArray,
        capPreds: LongArray?,
        segPreds: LongArray?,
        sentencePiece: SentencePiece
    ): IntArray {
        val result = mutableListOf<Int>()

        Log.d("OnnxPunctuator", "Начало обработки. inputIds.size = ${inputIds.size}")

        // Обрабатываем все токены, включая первый и последний
        for (i in inputIds.indices) {
            val tokenId = inputIds[i]

            // Проверяем границы массивов
            if (i >= prePreds.size || i >= postPreds.size) {
                Log.w("OnnxPunctuator", "Индекс $i выходит за границы массивов предсказаний")
                result.add(tokenId)
                continue
            }

            // Получаем предсказания для текущего токена
            val preIdx = prePreds[i].toInt()
            val postIdx = postPreds[i].toInt()

            Log.v("OnnxPunctuator", "Токен $i: ID=$tokenId, preIdx=$preIdx, postIdx=$postIdx")

            // 1. Добавляем предшествующую пунктуацию (кроме первого токена)
            if (i > 0 && preIdx > 0 && preIdx < preLabels.size) {
                val prePunct = preLabels[preIdx]
                if (prePunct != nullToken) {
                    // Кодируем знак пунктуации через SentencePiece
                    try {
                        val punctIds = sentencePiece.encode(prePunct)
                        result.addAll(punctIds.toList())
                        Log.v("OnnxPunctuator", "Добавлена предшествующая пунктуация: $prePunct (${punctIds.size} токенов)")
                    } catch (e: Exception) {
                        Log.e("OnnxPunctuator", "Ошибка кодирования пунктуации: $prePunct", e)
                    }
                }
            }

            // 2. Добавляем текущий токен (если это не BOS или EOS)
            if (tokenId != 1 && tokenId != 2) {
                result.add(tokenId)
            }

            // 3. Добавляем последующую пунктуацию (кроме последнего токена)
            if (i < inputIds.size - 1 && postIdx > 0 && postIdx < postLabels.size) {
                val postPunct = postLabels[postIdx]
                if (postPunct != nullToken) {
                    // Кодируем знак пунктуации через SentencePiece
                    try {
                        val punctIds = sentencePiece.encode(postPunct)
                        result.addAll(punctIds.toList())
                        Log.v("OnnxPunctuator", "Добавлена последующая пунктуация: $postPunct (${punctIds.size} токенов)")
                    } catch (e: Exception) {
                        Log.e("OnnxPunctuator", "Ошибка кодирования пунктуации: $postPunct", e)
                    }
                }
            }
        }

        Log.d("OnnxPunctuator", "Обработка завершена. result.size = ${result.size}")
        return result.toIntArray()
    }*/
    /*private fun processPredictions(
        inputIds: IntArray,
        prePreds: LongArray,
        postPreds: LongArray,
        capPreds: LongArray?,
        segPreds: LongArray?,
        sentencePiece: SentencePiece
    ): IntArray {
        val result = mutableListOf<Int>()

        Log.d("OnnxPunctuator", "Начало обработки. inputIds.size = ${inputIds.size}")

        // Временный список для хранения токенов с их пост-пунктуацией
        val tokensWithPostPunct = mutableListOf<Pair<Int, String?>>()

        // 1. Собираем токены с пост-пунктуацией
        for (i in inputIds.indices) {
            val tokenId = inputIds[i]

            // Пропускаем BOS и EOS токены
            if (tokenId == 1 || tokenId == 2) {
                continue
            }

            // Проверяем границы массивов
            if (i >= prePreds.size || i >= postPreds.size) {
                tokensWithPostPunct.add(Pair(tokenId, null))
                continue
            }

            // Получаем пост-пунктуацию
            val postIdx = postPreds[i].toInt()
            val postPunct = if (postIdx > 0 && postIdx < postLabels.size) {
                postLabels[postIdx]
            } else {
                nullToken
            }

            // Добавляем токен с пост-пунктуацией
            tokensWithPostPunct.add(Pair(tokenId, if (postPunct != nullToken) postPunct else null))
        }

        Log.d("OnnxPunctuator", "Собрано ${tokensWithPostPunct.size} токенов")

        // 2. Обрабатываем токены, добавляя пре-пунктуацию и токены
        for (i in tokensWithPostPunct.indices) {
            val (tokenId, postPunct) = tokensWithPostPunct[i]

            // Добавляем пре-пунктуацию (используем prePreds из соответствующего индекса)
            // Находим соответствующий индекс в inputIds (с учетом пропущенных BOS/EOS)
            val originalIndex = i + 1 // +1 потому что пропустили BOS

            if (originalIndex < prePreds.size) {
                val preIdx = prePreds[originalIndex].toInt()
                if (preIdx > 0 && preIdx < preLabels.size) {
                    val prePunct = preLabels[preIdx]
                    if (prePunct != nullToken) {
                        // Проверяем, чтобы не было двойной пунктуации
                        // Если перед этим уже была добавлена запятая, не добавляем еще одну
                        if (!(prePunct == "," && i > 0 && tokensWithPostPunct[i - 1].second == ",")) {
                            try {
                                val punctIds = sentencePiece.encode(prePunct)
                                // Убираем лишний пробел перед знаком препинания
                                if (punctIds.isNotEmpty()) {
                                    result.addAll(punctIds.toList())
                                    Log.v("OnnxPunctuator", "Добавлена пре-пунктуация: $prePunct")
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "OnnxPunctuator",
                                    "Ошибка кодирования пре-пунктуации: $prePunct",
                                    e
                                )
                            }
                        }
                    }
                }
            }

            // Добавляем текущий токен
            result.add(tokenId)

            // Добавляем пост-пунктуацию
            if (postPunct != null) {
                // Проверяем, чтобы не ставить запятую, если следующая пре-пунктуация тоже запятая
                val nextHasCommaPre = if (i + 1 < tokensWithPostPunct.size) {
                    val nextOriginalIndex = i + 2
                    nextOriginalIndex < prePreds.size && prePreds[nextOriginalIndex].toInt() > 0 &&
                            preLabels[prePreds[nextOriginalIndex].toInt()] == ","
                } else false

                if (!(postPunct == "," && nextHasCommaPre)) {
                    try {
                        val punctIds = sentencePiece.encode(postPunct)
                        if (punctIds.isNotEmpty()) {
                            result.addAll(punctIds.toList())
                            Log.v("OnnxPunctuator", "Добавлена пост-пунктуация: $postPunct")
                        }
                    } catch (e: Exception) {
                        Log.e("OnnxPunctuator", "Ошибка кодирования пост-пунктуации: $postPunct", e)
                    }
                }
            }

            // Обрабатываем границы предложений
            segPreds?.let {
                if (originalIndex < it.size && it[originalIndex] > 0L && i < tokensWithPostPunct.size - 1) {
                    // Проверяем, не добавлена ли уже точка как пост-пунктуация
                    if (postPunct != ".") {
                        try {
                            val dotIds = sentencePiece.encode(".")
                            if (dotIds.isNotEmpty()) {
                                result.addAll(dotIds.toList())
                                Log.v("OnnxPunctuator", "Добавлена точка в конце предложения")
                            }
                        } catch (e: Exception) {
                            Log.e("OnnxPunctuator", "Ошибка добавления точки", e)
                        }
                    }
                }
            }
        }

        Log.d("OnnxPunctuator", "Обработка завершена. result.size = ${result.size}")

        // 3. Пост-обработка: убираем лишние пробелы перед знаками препинания
        return postProcessTokens(result, sentencePiece)
    }*/
    private fun processPredictions(
        inputIds: IntArray,
        prePreds: LongArray,
        postPreds: LongArray,
        capPreds: LongArray?,
        segPreds: LongArray?,
        sentencePiece: SentencePiece
    ): IntArray {
        val result = mutableListOf<Int>()

        Log.d("OnnxPunctuator", "Начало обработки. inputIds.size = ${inputIds.size}")

        // Собираем токены с их пунктуацией
        for (i in inputIds.indices) {
            val tokenId = inputIds[i]

            // Пропускаем BOS и EOS токены
            if (tokenId == 1 || tokenId == 2) {
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

            // Добавляем пре-пунктуацию (только если она есть и не равна NULL)
            if (preIdx > 0 && preIdx < preLabels.size) {
                val prePunct = preLabels[preIdx]
                if (prePunct != nullToken && prePunct != "NULL" && prePunct != "<NULL>") {
                    // Проверяем, чтобы не дублировать пунктуацию
                    if (result.isNotEmpty()) {
                        val lastToken = result.last()
                        try {
                            val lastTokenText = sentencePiece.decode(intArrayOf(lastToken))
                            // Если последний токен уже является знаком препинания, не добавляем еще один
                            if (lastTokenText !in punctuationMarks) {
                                addPunctuation(prePunct, result, sentencePiece)
                            }
                        } catch (e: Exception) {
                            addPunctuation(prePunct, result, sentencePiece)
                        }
                    } else {
                        addPunctuation(prePunct, result, sentencePiece)
                    }
                }
            }

            // Добавляем текущий токен
            result.add(tokenId)

            // Добавляем пост-пунктуацию
            if (postIdx > 0 && postIdx < postLabels.size) {
                val postPunct = postLabels[postIdx]
                if (postPunct != nullToken && postPunct != "NULL" && postPunct != "<NULL>") {
                    addPunctuation(postPunct, result, sentencePiece)
                }
            }

            // Проверяем границы предложений
            segPreds?.let {
                if (i < it.size && it[i] > 0L && i < inputIds.size - 2) {
                    // Добавляем точку, если она не была добавлена как пост-пунктуация
                    addPunctuation(".", result, sentencePiece)
                }
            }
        }

        Log.d("OnnxPunctuator", "Обработка завершена. result.size = ${result.size}")

        // Пост-обработка для улучшения форматирования
        return postProcessTokens(result, sentencePiece)
    }

    private fun addPunctuation(punctuation: String, result: MutableList<Int>, sentencePiece: SentencePiece) {
        try {
            val punctIds = sentencePiece.encode(punctuation)
            if (punctIds.isNotEmpty()) {
                result.addAll(punctIds.toList())
                Log.v("OnnxPunctuator", "Добавлена пунктуация: $punctuation")
            }
        } catch (e: Exception) {
            Log.e("OnnxPunctuator", "Ошибка кодирования пунктуации: $punctuation", e)
        }
    }

    // Список знаков препинания для проверки
    private val punctuationMarks = setOf(
        ".", ",", "?", "!", ";", ":", "…", "—", "-", "–",
        ")", "]", "}", "\"", "»", "“", "？", "，", "。", "、",
        "・", "।", "؟", "،", "።", "፣", "፧"
    )
    private fun postProcessTokens(tokens: List<Int>, sentencePiece: SentencePiece): IntArray {
        if (tokens.isEmpty()) return intArrayOf()

        // Декодируем токены в текст
        val text = try {
            sentencePiece.decode(tokens.toIntArray())
        } catch (e: Exception) {
            Log.e("OnnxPunctuator", "Ошибка декодирования при пост-обработке", e)
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
            sentencePiece.encode(processedText)
        } catch (e: Exception) {
            Log.e("OnnxPunctuator", "Ошибка кодирования при пост-обработке", e)
            tokens.toIntArray()
        }
    }

    private fun addSpaceAfterComma(text: String): String {
        // Используем регулярное выражение для добавления пробела после запятой, точки с запятой, двоеточия
        var result = text

        // Паттерн: знак препинания, затем буква (без пробела между ними)
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

                // Делаем первую букву заглавной, если она строчная и это русская/английская буква
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