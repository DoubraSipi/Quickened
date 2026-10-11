cmake_minimum_required(VERSION 3.22.1)

project(quickened_espeak C)

# Vendored espeak-ng 1.52.0 sources (phonemize-only static lib for Piper).
set(ESPEAK_DIR ${CMAKE_CURRENT_SOURCE_DIR}/espeak-ng)

file(GLOB ESPEAK_LIB_SRCS
    "${ESPEAK_DIR}/lib/*.c"
)
# Drop async/mbrola/klatt/speech-player sources (features off in config.h).
list(FILTER ESPEAK_LIB_SRCS EXCLUDE REGEX ".*/(event|fifo|espeak_command|mbrowrap|compilembrola|synth_mbrola|klatt|sPlayer)\\.c$")

file(GLOB ESPEAK_UCD_SRCS "${ESPEAK_DIR}/ucd/*.c")

add_library(espeak-ng SHARED
    ${ESPEAK_LIB_SRCS}
    ${ESPEAK_UCD_SRCS}
    espeak_jni.c
)

target_include_directories(espeak-ng PRIVATE
    ${ESPEAK_DIR}
    ${ESPEAK_DIR}/include
    ${ESPEAK_DIR}/lib
    ${ESPEAK_DIR}/ucd
)

target_compile_definitions(espeak-ng PRIVATE
    LIBESPEAK_NG_EXPORT=1
    PATH_ESPEAK_DATA="/unused"
)

target_compile_options(espeak-ng PRIVATE
    -fPIC
    -fvisibility=hidden
    -fno-exceptions
    -fwrapv
    -Wno-unused-parameter
    -Wno-unused
    -Wno-implicit
)

find_library(log-lib log)
target_link_libraries(espeak-ng m ${log-lib})
