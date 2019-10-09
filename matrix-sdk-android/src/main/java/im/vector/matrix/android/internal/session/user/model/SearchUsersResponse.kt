package im.vector.matrix.android.internal.session.user.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing an users search response
 */
@JsonClass(generateAdapter = true)
internal data class SearchUsersRequestResponse(
        @Json(name = "limited") val limited: Boolean = false,
        @Json(name = "results") val users: List<SearchUser> = emptyList()
)
