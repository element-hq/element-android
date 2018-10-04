package im.vector.matrix.android.api.login.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HomeServerConnectionConfig(
        // the home server URI
        val hsUri: String
)