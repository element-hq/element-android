package im.vector.matrix.android.internal.session.sync.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class TokensChunkResponse<T>(
        val start: String? = null,
        val end: String? = null,
        val chunk: List<T>? = null)
