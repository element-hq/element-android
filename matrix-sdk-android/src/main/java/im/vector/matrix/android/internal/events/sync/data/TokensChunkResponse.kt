package im.vector.matrix.android.internal.events.sync.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokensChunkResponse<T>(
        val start: String? = null,
        val end: String? = null,
        val chunk: List<T>? = null)
