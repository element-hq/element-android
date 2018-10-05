package im.vector.matrix.android.api.auth.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HomeServerConnectionConfig(
        // the home server URI
        val hsUri: String
)