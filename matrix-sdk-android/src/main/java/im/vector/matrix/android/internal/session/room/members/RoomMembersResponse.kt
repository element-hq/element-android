package im.vector.matrix.android.internal.session.room.members

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.Event

@JsonClass(generateAdapter = true)
data class RoomMembersResponse(
        @Json(name = "chunk") val roomMemberEvents: List<Event>
)