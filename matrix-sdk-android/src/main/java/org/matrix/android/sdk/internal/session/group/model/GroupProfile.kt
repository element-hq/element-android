/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.group.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class represents a community profile in the server responses.
 */
@JsonClass(generateAdapter = true)
internal data class GroupProfile(

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
