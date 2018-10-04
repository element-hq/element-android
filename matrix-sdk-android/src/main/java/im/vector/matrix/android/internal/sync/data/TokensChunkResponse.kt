package im.vector.matrix.android.internal.sync.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokensChunkResponse<T>(
        var start: String? = null,
        var end: String? = null,
        var chunk: List<T>? = null)
