package im.vector.matrix.android.internal.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class InvitedGroupSync(
        /**
         * The identifier of the inviter.
         */
        @Json(name = "inviter") val inviter: String? = null,

        /**
         * The group profile.
         */
        @Json(name = "profile") val profile: GroupSyncProfile? = null
)