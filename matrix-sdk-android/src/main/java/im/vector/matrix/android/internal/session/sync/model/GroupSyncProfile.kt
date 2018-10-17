package im.vector.matrix.android.internal.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroupSyncProfile(
        /**
         * The name of the group, if any. May be nil.
         */
        @Json(name = "name") var name: String? = null,

        /**
         * The URL for the group's avatar. May be nil.
         */
        @Json(name = "avatar_url") var avatarUrl: String? = null
)