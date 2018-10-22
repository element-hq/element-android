package im.vector.matrix.android.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageContent(

        @Json(name = "msgtype") val type: String? = null,
        @Json(name = "body") val body: String? = null,
        @Json(name = "format") val format: String? = null,
        @Json(name = "formatted_body") val formattedBody: String? = null

)