package im.vector.matrix.android.internal.session.group.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GroupRoom(

        @Json(name = "aliases") val aliases: List<String> = emptyList(),
        @Json(name = "canonical_alias") val canonicalAlias: String? = null,
        @Json(name = "name") val name: String? = null,
        @Json(name = "num_joined_members") val numJoinedMembers: Int = 0,
        @Json(name = "room_id") val roomId: String,
        @Json(name = "topic") val topic: String? = null,
        @Json(name = "world_readable") val worldReadable: Boolean = false,
        @Json(name = "guest_can_join") val guestCanJoin: Boolean = false,
        @Json(name = "avatar_url") val avatarUrl: String? = null

)
