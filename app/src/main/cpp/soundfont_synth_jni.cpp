#include <jni.h>
#include <android/log.h>
#include <vector>
#include <mutex>

#define TSF_IMPLEMENTATION
#include "tsf.h"

namespace {
const char* kLogTag = "SoundFontSynth";

struct SynthState {
    tsf* soundfont = nullptr;
    int sampleRate = 44100;
    float volume = 1.0f;
    std::mutex mutex;
};

SynthState* fromHandle(jlong handle) {
    return reinterpret_cast<SynthState*>(handle);
}

void logError(const char* message) {
    __android_log_print(ANDROID_LOG_ERROR, kLogTag, "%s", message);
}
} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_xianliticn_yuefu_music_SoundFontSynth_nativeCreate(JNIEnv* env, jobject /*thiz*/, jint sampleRate) {
    auto* state = new SynthState();
    if (sampleRate > 0) {
        state->sampleRate = sampleRate;
    }
    return reinterpret_cast<jlong>(state);
}

extern "C" JNIEXPORT void JNICALL
Java_com_xianliticn_yuefu_music_SoundFontSynth_nativeDestroy(JNIEnv* env, jobject /*thiz*/, jlong handle) {
    auto* state = fromHandle(handle);
    if (!state) return;

    {
        std::lock_guard<std::mutex> lock(state->mutex);
        if (state->soundfont) {
            tsf_close(state->soundfont);
            state->soundfont = nullptr;
        }
    }

    delete state;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_xianliticn_yuefu_music_SoundFontSynth_nativeLoadSoundFont(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jbyteArray data
) {
    auto* state = fromHandle(handle);
    if (!state || !data) return JNI_FALSE;

    const jsize length = env->GetArrayLength(data);
    if (length <= 0) return JNI_FALSE;

    std::vector<jbyte> buffer(static_cast<size_t>(length));
    env->GetByteArrayRegion(data, 0, length, buffer.data());

    std::lock_guard<std::mutex> lock(state->mutex);
    if (state->soundfont) {
        tsf_close(state->soundfont);
        state->soundfont = nullptr;
    }

    state->soundfont = tsf_load_memory(buffer.data(), length);
    if (!state->soundfont) {
        logError("Failed to load SoundFont from memory.");
        return JNI_FALSE;
    }

    tsf_set_output(state->soundfont, TSF_STEREO_INTERLEAVED, state->sampleRate, 0.0f);
    tsf_set_max_voices(state->soundfont, 256);
    tsf_set_volume(state->soundfont, state->volume);

    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_xianliticn_yuefu_music_SoundFontSynth_nativeSetVolume(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jfloat volume
) {
    auto* state = fromHandle(handle);
    if (!state) return;
    if (volume < 0.0f) volume = 0.0f;

    std::lock_guard<std::mutex> lock(state->mutex);
    state->volume = volume;
    if (state->soundfont) {
        tsf_set_volume(state->soundfont, state->volume);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_xianliticn_yuefu_music_SoundFontSynth_nativeSetPreset(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jint channel,
    jint preset
) {
    auto* state = fromHandle(handle);
    if (!state) return;

    std::lock_guard<std::mutex> lock(state->mutex);
    if (!state->soundfont) return;

    tsf_channel_set_presetnumber(state->soundfont, channel, preset, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_xianliticn_yuefu_music_SoundFontSynth_nativeNoteOn(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jint channel,
    jint key,
    jfloat velocity
) {
    auto* state = fromHandle(handle);
    if (!state) return;

    if (velocity < 0.0f) velocity = 0.0f;
    if (velocity > 1.0f) velocity = 1.0f;

    std::lock_guard<std::mutex> lock(state->mutex);
    if (!state->soundfont) return;

    tsf_channel_note_on(state->soundfont, channel, key, velocity);
}

extern "C" JNIEXPORT void JNICALL
Java_com_xianliticn_yuefu_music_SoundFontSynth_nativeNoteOff(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jint channel,
    jint key
) {
    auto* state = fromHandle(handle);
    if (!state) return;

    std::lock_guard<std::mutex> lock(state->mutex);
    if (!state->soundfont) return;

    tsf_channel_note_off(state->soundfont, channel, key);
}

extern "C" JNIEXPORT void JNICALL
Java_com_xianliticn_yuefu_music_SoundFontSynth_nativeAllNotesOff(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle
) {
    auto* state = fromHandle(handle);
    if (!state) return;

    std::lock_guard<std::mutex> lock(state->mutex);
    if (!state->soundfont) return;

    tsf_note_off_all(state->soundfont);
}

extern "C" JNIEXPORT void JNICALL
Java_com_xianliticn_yuefu_music_SoundFontSynth_nativeAllSoundOff(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle
) {
    auto* state = fromHandle(handle);
    if (!state) return;

    std::lock_guard<std::mutex> lock(state->mutex);
    if (!state->soundfont) return;

    for (int channel = 0; channel < 16; ++channel) {
        tsf_channel_sounds_off_all(state->soundfont, channel);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_xianliticn_yuefu_music_SoundFontSynth_nativeRender(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong handle,
    jshortArray buffer,
    jint frames
) {
    auto* state = fromHandle(handle);
    if (!state || !buffer || frames <= 0) return 0;

    const jsize bufferLen = env->GetArrayLength(buffer);
    const jint requiredSamples = frames * 2;
    if (bufferLen < requiredSamples) return 0;

    jshort* output = env->GetShortArrayElements(buffer, nullptr);
    if (!output) return 0;

    {
        std::lock_guard<std::mutex> lock(state->mutex);
        if (state->soundfont) {
            tsf_render_short(state->soundfont, output, frames, 0);
        } else {
            memset(output, 0, sizeof(jshort) * requiredSamples);
        }
    }

    env->ReleaseShortArrayElements(buffer, output, 0);
    return frames;
}
