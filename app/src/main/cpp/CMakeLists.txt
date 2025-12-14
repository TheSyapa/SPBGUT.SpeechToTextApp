cmake_minimum_required(VERSION 3.18.1)

project("sentencepiece_jni")

# Добавьте SentencePiece как подпроект
add_subdirectory(sentencepiece)

# Создайте нативную библиотеку
add_library(
        sentencepiece_jni
        SHARED
        jni_funcs_sentencepiece.cpp
)

# Свяжите с SentencePiece
target_link_libraries(
        sentencepiece_jni
        sentencepiece
        log
)

# Для C++17
set_target_properties(
        sentencepiece_jni
        PROPERTIES
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON
)