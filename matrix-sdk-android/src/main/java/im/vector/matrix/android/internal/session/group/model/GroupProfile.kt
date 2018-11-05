package im.vector.matrix.android.internal.session.group.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class represents a community profile in the server responses.
 */
@JsonClass(generateAdapter = true)
data class GroupProfile(

        @Json(name = "short_description") val shortDescription: String? = null,

        /**
         * Tell whether the group is public.
         */
        @Json(name = "is_public") val isPublic: Boolean? = null,

        /**
         * The URL for the group's avatar. May be nil.
         */
        @Json(name = "avatar_url") val avatarUrl: String? = null,

        /**
         * The group's name.
         */
        @Json(name = "name") val name: String? = null,

        /**
         * The optional HTML formatted string used to described the group.
         */
        @Json(name = "long_description") val longDescription: String? = null
)
