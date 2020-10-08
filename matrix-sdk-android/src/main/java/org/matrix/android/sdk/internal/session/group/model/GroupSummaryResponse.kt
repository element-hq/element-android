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
 * This class represents the summary of a community in the server response.
 */
@JsonClass(generateAdapter = true)
internal data class GroupSummaryResponse(
        /**
         * The group profile.
         */
        @Json(name = "profile") val profile: GroupProfile? = null,

        /**
         * The group users.
         */
        @Json(name = "users_section") val usersSection: GroupSummaryUsersSection? = null,

        /**
         * The current user status.
         */
        @Json(name = "user") val user: GroupSummaryUser? = null,

        /**
         * The rooms linked to the community.
         */
        @Json(name = "rooms_section") val roomsSection: GroupSummaryRoomsSection? = null
)
