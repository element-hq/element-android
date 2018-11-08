package im.vector.matrix.android.internal.session.group.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class represents the summary of a community in the server response.
 */
@JsonClass(generateAdapter = true)
internal data class GroupSummaryResponse(
        /**
         * The group profile.
         */
        @Json(name = "profile") val profile: GroupProfile? = null,

        /**
         * The group users.
         */
        @Json(name = "users_section") val usersSection: GroupSummaryUsersSection? = null,

        /**
         * The current user status.
         */
        @Json(name = "user") val user: GroupSummaryUser? = null,

        /**
         * The rooms linked to the community.
         */
        @Json(name = "rooms_section") val roomsSection: GroupSummaryRoomsSection? = null
)
