package com.xianliticn.yuefu.webapi.lyria

import kotlinx.serialization.Serializable

@Serializable
data class LyriaResponse(
    val serverContent: ServerContent? = null
)

@Serializable
data class ServerContent(
    val audioChunks: List<AudioChunk>? = null
)

@Serializable
data class AudioChunk(
    val data: String,
    val sourceMetadata: SourceMetadata? = null,
    val mimeType: String? = null
)

@Serializable
data class SourceMetadata(
    val clientContent: ClientContent? = null,
    val musicGenerationConfig: MusicGenerationConfigMetadata? = null
)

@Serializable
data class MusicGenerationConfigMetadata(
    val seed: Long? = null
)
