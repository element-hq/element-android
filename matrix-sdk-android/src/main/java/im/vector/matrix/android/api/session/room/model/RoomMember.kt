package im.vector.matrix.android.api.session.room.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.session.events.model.UnsignedData

@JsonClass(generateAdapter = true)
data class RoomMember(
        @Json(name = "membership") val membership: Membership,
        @Json(name = "display_name") val displayDame: String? = null,
        @Json(name = "avatar_url") val avatarUrl: String? = null,
        @Json(name = "is_direct") val isDirect: Boolean = false,
        @Json(name = "third_party_invite") val thirdPartyInvite: Invite? = null,
        @Json(name = "unsigned_data") val unsignedData: UnsignedData? = null
)
