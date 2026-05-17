cmake_minimum_required(VERSION 3.22.1)

project(yuefu_synth)


add_library(yuefu_synth SHARED
    soundfont_synth_jni.cpp
)

find_library(log-lib log)

# TinySoundFont is header-only; implementation is compiled in the JNI source.
target_include_directories(yuefu_synth PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

target_link_libraries(yuefu_synth ${log-lib})
