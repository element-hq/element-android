package im.vector.matrix.android.internal.session.group.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


/**
 * This class represents the community members in a group summary response.
 */

@JsonClass(generateAdapter = true)
data class GroupSummaryUsersSection(

        @Json(name = "total_user_count_estimate") val totalUserCountEstimate: Int,

        @Json(name = "users") val users: List<String> = emptyList()

        // @TODO: Check the meaning and the usage of these roles. This dictionary is empty FTM.
        //public Map<Object, Object> roles;
)
