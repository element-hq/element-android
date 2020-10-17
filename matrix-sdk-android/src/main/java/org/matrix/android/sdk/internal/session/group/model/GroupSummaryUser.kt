/*
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
