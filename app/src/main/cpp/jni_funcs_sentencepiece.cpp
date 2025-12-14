#include <jni.h>
#include <string>
#include <vector>
#include "sentencepiece/src/sentencepiece_processor.h"

#ifdef __cplusplus
extern "C" {
#endif

// Структура для хранения указателя
struct SentencePieceHandle {
    sentencepiece::SentencePieceProcessor* processor;
};

// Инициализация (создает только структуру)
JNIEXPORT jlong JNICALL
Java_syapa_spbgut_speechtotext_processor_SentencePieceNative_initNative(
        JNIEnv* /* env */,
        jobject /* this */) {

    SentencePieceHandle* handle = new SentencePieceHandle();
    handle->processor = nullptr; // Пока не создаем сам процессор
    return reinterpret_cast<jlong>(handle);
}

// Загрузка модели (создает процессор и загружает модель)
JNIEXPORT jboolean JNICALL
Java_syapa_spbgut_speechtotext_processor_SentencePieceNative_loadModelNative(
        JNIEnv* env,
        jobject /* this */,
        jlong handlePtr,
        jstring modelPath) {

    SentencePieceHandle* handle = reinterpret_cast<SentencePieceHandle*>(handlePtr);
    if (handle == nullptr) {
        return JNI_FALSE;
    }

    // Создаем процессор, если еще не создан
    if (handle->processor == nullptr) {
        handle->processor = new sentencepiece::SentencePieceProcessor();
    }

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    const auto status = handle->processor->Load(path);
    env->ReleaseStringUTFChars(modelPath, path);

    return status.ok() ? JNI_TRUE : JNI_FALSE;
}

// Кодирование текста
JNIEXPORT jintArray JNICALL
Java_syapa_spbgut_speechtotext_processor_SentencePieceNative_encodeNative(
        JNIEnv* env,
        jobject /* this */,
        jlong handlePtr,
        jstring text) {

    SentencePieceHandle* handle = reinterpret_cast<SentencePieceHandle*>(handlePtr);
    if (handle == nullptr || handle->processor == nullptr) {
        return nullptr;
    }

    const char* input = env->GetStringUTFChars(text, nullptr);
    std::vector<int> ids;
    handle->processor->Encode(input, &ids);
    env->ReleaseStringUTFChars(text, input);

    jintArray result = env->NewIntArray(static_cast<jsize>(ids.size()));
    env->SetIntArrayRegion(result, 0, ids.size(), ids.data());

    return result;
}

// Декодирование ID
JNIEXPORT jstring JNICALL
Java_syapa_spbgut_speechtotext_processor_SentencePieceNative_decodeNative(
        JNIEnv* env,
        jobject /* this */,
        jlong handlePtr,
        jintArray idsArray) {

    SentencePieceHandle* handle = reinterpret_cast<SentencePieceHandle*>(handlePtr);
    if (handle == nullptr || handle->processor == nullptr) {
        return env->NewStringUTF("");
    }

    jsize length = env->GetArrayLength(idsArray);
    jint* ids = env->GetIntArrayElements(idsArray, nullptr);

    std::vector<int> vec_ids(ids, ids + length);
    std::string text;
    handle->processor->Decode(vec_ids, &text);

    env->ReleaseIntArrayElements(idsArray, ids, 0);

    return env->NewStringUTF(text.c_str());
}

// Освобождение ресурсов
JNIEXPORT void JNICALL
Java_syapa_spbgut_speechtotext_processor_SentencePieceNative_releaseNative(
        JNIEnv* /* env */,
        jobject /* this */,
        jlong handlePtr) {

    SentencePieceHandle* handle = reinterpret_cast<SentencePieceHandle*>(handlePtr);
    if (handle != nullptr) {
        if (handle->processor != nullptr) {
            delete handle->processor;
        }
        delete handle;
    }
}

#ifdef __cplusplus
}
#endif