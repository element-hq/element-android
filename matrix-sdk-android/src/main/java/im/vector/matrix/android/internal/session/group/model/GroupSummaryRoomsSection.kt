package im.vector.matrix.android.internal.session.group.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class represents the community rooms in a group summary response.
 */
@JsonClass(generateAdapter = true)
data class GroupSummaryRoomsSection(

        @Json(name = "total_room_count_estimate") val totalRoomCountEstimate: Int? = null,

        @Json(name = "rooms") val rooms: List<String> = emptyList()

        // @TODO: Check the meaning and the usage of these categories. This dictionary is empty FTM.
        //public Map<Object, Object> categories;
)
