package im.vector.matrix.android.internal.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class GroupsSyncResponse(
        /**
         * Joined groups: An array of groups ids.
         */
        @Json(name = "join") val join: Map<String, Any> = emptyMap(),

        /**
         * Invitations. The groups that the user has been invited to: keys are groups ids.
         */
        @Json(name = "invite") val invite: Map<String, InvitedGroupSync> = emptyMap(),

        /**
         * Left groups. An array of groups ids: the groups that the user has left or been banned from.
         */
        @Json(name = "leave") val leave: Map<String, Any> = emptyMap()
)