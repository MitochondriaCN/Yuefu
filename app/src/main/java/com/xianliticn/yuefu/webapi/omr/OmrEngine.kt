package com.xianliticn.yuefu.webapi.omr

import kotlinx.serialization.Serializable

@Serializable
enum class OmrEngine {
    AUDIVERIS,
    LEGATO_FP16,
    LEGATO_FP32
}
