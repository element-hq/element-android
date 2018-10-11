package im.vector.matrix.android.api.events

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.legacy.rest.model.pid.RoomThirdPartyInvite

/**
 * Class representing an event content
 */
@JsonClass(generateAdapter = true)
data class EventContent(
        /**
         * The display name for this user, if any.
         */
        @Json(name = "displayname") val displayName: String? = null,

        /**
         * The avatar URL for this user, if any.
         */
        @Json(name = "avatar_url") val avatarUrl: String? = null,

        /**
         * The membership state of the user. One of: ["invite", "join", "knock", "leave", "ban"]
         */
        @Json(name = "membership") val membership: String? = null,

        /**
         * the third party invite
         */
        @Json(name = "third_party_invite") val thirdPartyInvite: RoomThirdPartyInvite? = null,

        /*
         * e2e encryption format
         */
        @Json(name = "algorithm") val algorithm: String? = null
)