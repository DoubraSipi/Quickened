// JNI bridge: espeak-ng text -> IPA phoneme string for Piper.
// Lifecycle: init once per process with the espeak-ng-data path,
// then phonemize() per utterance. Mapping phoneme symbols to Piper
// model IDs happens in Kotlin using the voice's phoneme_id_map.
#include <jni.h>
#include <stdlib.h>
#include <string.h>

#include <espeak-ng/speak_lib.h>

static int g_inited = 0;

JNIEXPORT jint JNICALL
Java_com_quickened_piper_EspeakBridge_nativeInit(JNIEnv *env, jclass clazz, jstring dataPath) {
    if (g_inited) return 0;
    const char *path = (*env)->GetStringUTFChars(env, dataPath, 0);
    int rate = espeak_Initialize(AUDIO_OUTPUT_SYNCHRONOUS, 0, path, 0);
    (*env)->ReleaseStringUTFChars(env, dataPath, path);
    if (rate <= 0) return -1;
    if (espeak_SetVoiceByName("en") != EE_OK) return -2;
    g_inited = 1;
    return rate;
}

JNIEXPORT jstring JNICALL
Java_com_quickened_piper_EspeakBridge_nativePhonemize(JNIEnv *env, jclass clazz, jstring voice, jstring text) {
    if (!g_inited) return NULL;
    const char *v = (*env)->GetStringUTFChars(env, voice, 0);
    const char *t = (*env)->GetStringUTFChars(env, text, 0);
    if (espeak_SetVoiceByName(v) != EE_OK) {
        (*env)->ReleaseStringUTFChars(env, voice, v);
        (*env)->ReleaseStringUTFChars(env, text, t);
        return NULL;
    }
    // Accumulate clause phonemes; espeak advances the pointer, NULL ends.
    char buf[16384];
    size_t pos = 0;
    buf[0] = '\0';
    const void *p = t;
    for (int guard = 0; guard < 256; guard++) {
        const char *ph = espeak_TextToPhonemes(&p, espeakCHARS_AUTO, espeakPHONEMES_IPA);
        if (ph == NULL || ph[0] == '\0') break;
        size_t n = strlen(ph);
        if (pos + n + 2 >= sizeof(buf)) break;
        memcpy(buf + pos, ph, n);
        pos += n;
        buf[pos++] = ' ';
        buf[pos] = '\0';
        if (p == NULL) break;
    }
    jstring result = (*env)->NewStringUTF(env, buf);
    (*env)->ReleaseStringUTFChars(env, voice, v);
    (*env)->ReleaseStringUTFChars(env, text, t);
    return result;
}
