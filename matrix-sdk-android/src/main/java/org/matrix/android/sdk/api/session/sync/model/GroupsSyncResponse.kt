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

package org.matrix.android.sdk.api.session.sync.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroupsSyncResponse(
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
