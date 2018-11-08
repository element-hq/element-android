package im.vector.matrix.android.internal.session.group.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class represents the current user status in a group summary response.
 */
@JsonClass(generateAdapter = true)
internal data class GroupSummaryUser(

        /**
         * The current user membership in this community.
         */
        @Json(name = "membership") val membership: String? = null,

        /**
         * Tell whether the user published this community on his profile.
         */
        @Json(name = "is_publicised") val isPublicised: Boolean? = null
)
