package im.vector.matrix.android.api.session.room.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageDefaultContent(
        @Json(name = "msgtype") override val type: String,
        @Json(name = "body") override val body: String
) : MessageContent