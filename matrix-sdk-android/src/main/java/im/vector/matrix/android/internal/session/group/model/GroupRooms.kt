package im.vector.matrix.android.internal.session.group.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroupRooms(

        @Json(name = "total_room_count_estimate") val totalRoomCountEstimate: Int? = null,
        @Json(name = "chunk") val rooms: List<GroupRoom> = emptyList()

)
