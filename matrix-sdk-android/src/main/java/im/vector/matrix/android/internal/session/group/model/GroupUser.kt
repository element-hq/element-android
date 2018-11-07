package im.vector.matrix.android.internal.session.group.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroupUser(
        @Json(name = "display_name") val displayName: String = "",
        @Json(name = "user_id") val userId: String,
        @Json(name = "is_privileged") val isPrivileged: Boolean = false,
        @Json(name = "avatar_url") val avatarUrl: String? = "",
        @Json(name = "is_public") val isPublic: Boolean = false
)