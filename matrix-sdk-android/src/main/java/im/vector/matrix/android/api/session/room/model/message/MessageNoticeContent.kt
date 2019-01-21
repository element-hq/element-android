package im.vector.matrix.android.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageNoticeContent(
        @Json(name = "msgtype") override val type: String,
        @Json(name = "body") override val body: String,
        @Json(name = "format") val format: String? = null,
        @Json(name = "formatted_body") val formattedBody: String? = null
) : MessageContent