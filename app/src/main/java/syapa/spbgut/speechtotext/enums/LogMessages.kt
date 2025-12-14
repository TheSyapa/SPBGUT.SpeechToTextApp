package syapa.spbgut.speechtotext.enums

enum class LogMessages(
    val logText: String,
    val toastText: String? = null
) {
    ACTIVITY_CREATED("MainActivity создан. Приложение готово к работе!!!"),
    ACTIVITY_DESTROYED("MainActivity уничтожен"),

    MANAGERS_INITIALIZATION("Инициализация менеджеров"),

    MODELS_LOADING_START("Начало загрузки моделей"),
    MODELS_LOADING_SUCCESS("Модели успешно загружены"),
    MODEL_COPY_SUCCESS("Модель скопирована из assets: %s"),

    INITIALIZATION_ERROR(
        "Ошибка при инициализации",
        "Ошибка инициализации. Перезапустите приложение."
    ),
    MODELS_LOADING_CRITICAL_ERROR(
        "КРИТИЧЕСКАЯ ОШИБКА: Не удалось загрузить модели",
        "Не удалось загрузить необходимые модели. Приложение может работать некорректно."
    ),
    MODEL_COPY_ERROR("Не удалось скопировать модель из assets: %s"),

    RECORDING_START("Запуск записи"),
    RECORDING_STOP("Остановка записи"),
    RECORDING_START_ERROR("Не удалось начать запись", "Не удалось начать запись"),
    RECORDING_STOP_ERROR("Ошибка при остановке записи"),

    RECOGNITION_READY("Распознавание готово к работе"),
    RECOGNITION_BEGIN_SPEECH("Начало речи обнаружено"),
    RECOGNITION_END_SPEECH("Конец речи обнаружено"),
    RECOGNITION_SUCCESS_AMOUNT("Текст успешно распознан: %s"),
    RECOGNITION_EVENT("Событие распознавания: %d"),

    RECOGNITION_AUDIO_ERROR("Ошибка аудиозаписи при распознавании"),
    RECOGNITION_PERMISSION_ERROR("Недостаточно прав для распознавания"),
    RECOGNITION_NETWORK_ERROR("Сетевая ошибка при распознавании"),
    RECOGNITION_NO_MATCH("Не удалось распознать речь"),
    RECOGNITION_BUSY("Речевой движок занят"),
    RECOGNITION_TIMEOUT("Таймаут распознавания - речь не обнаружена"),
    RECOGNITION_CLIENT_ERROR("Ошибка клиента распознавания"),
    RECOGNITION_UNKNOWN_ERROR("Неизвестная ошибка распознавания: %s"),
    RECOGNITION_UNAVAILABLE(
        "Распознавание речи недоступно на устройстве",
        "Распознавание речи недоступно"
    ),

    RECOGNIZER_RESET("SpeechRecognizer сброшен"),
    RECOGNIZER_DESTROYED("SpeechRecognizer уничтожен"),

    CLEAR_TEXT("Очистка текста"),
    TEXT_PROCESSING_ERROR("Ошибка обработки текста моделью", "Ошибка обработки текста"),

    COPY_TO_CLIPBOARD("Копирование в буфер обмена: %s"),
    COPY_TO_CLIPBOARD_ERROR("Не удалось скопировать в буфер обмена", "Не удалось скопировать"),

    SAVE_TO_FILE("Сохранение в файл: %s"),
    SAVE_TO_FILE_ERROR("Не удалось сохранить файл", "Не удалось сохранить"),
    SAVE_EMPTY_TEXT(
        "Попытка сохранения пустого текста",
        "Текст пуст"
    ),

    PERMISSION_RESULT("Получен результат запроса разрешений: requestCode=%d"),

    MENU_ERROR("Ошибка при показе меню"),

    DESTROY_ERROR("Ошибка при завершении работы"),

    ONNX_MODEL_LOADED("Модель ONNX загружена успешно"),
    ONNX_LOAD_ERROR("Ошибка загрузки модели ONNX"),
    ONNX_MODEL_NOT_LOADED("Модель ONNX не загружена. Сначала вызовите loadModel()"),

    ONNX_INFERENCE_FAILED("Не удалось получить результаты инференса"),
    ONNX_INFERENCE_ERROR("Ошибка инференса ONNX"),
    ONNX_RESULTS_RECEIVED("Результаты инференса получены: %d"),
    ONNX_INVALID_INPUT("Входные данные не содержат BOS/EOS."),
    ONNX_PREDICTIONS_MISSING("Не удалось получить предсказания pre_preds или post_preds"),

    ONNX_INPUT_SIZE("Размер inputIds: %d"),
    ONNX_PRE_SIZE("Размер prePreds: %d"),
    ONNX_POST_SIZE("Размер postPreds: %d"),
    ONNX_SIZE_MISMATCH("Размер inputIds не совпадает с размером prePreds"),

    ONNX_PROCESSING_START("Начало обработки. inputIds.size = %d"),
    ONNX_PROCESSING_END("Обработка завершена. result.size = %d"),
    ONNX_PUNCT_ADDED("Добавлена пунктуация: %s"),
    ONNX_PUNCT_ADDED_MULTIPLE("Добавлена пунктуация: %s"),
    ONNX_DECODE_ERROR("Ошибка декодирования при пост-обработке"),
    ONNX_ENCODE_ERROR("Ошибка кодирования при пост-обработке"),

    ONNX_PREDICTIONS_HEADER("=== ЛОГИРОВАНИЕ ПРЕДСКАЗАНИЙ ==="),
    ONNX_PREDICTIONS_FOOTER("=== КОНЕЦ ЛОГИРОВАНИЯ ==="),
    ONNX_PREDICTIONS_DETAILS("Предсказания: %s"),
    ONNX_NO_PREDICTIONS("Нет предсказаний пунктуации"),

    ONNX_PRE_PREDS_ARRAY("pre_preds массив: [%s]"),
    ONNX_POST_PREDS_ARRAY("postPreds массив: [%s]"),
    ONNX_CAP_PREDS_ARRAY("capPreds массив: [%s]"),
    ONNX_SEG_PREDS_ARRAY("segPreds массив: [%s]"),
}