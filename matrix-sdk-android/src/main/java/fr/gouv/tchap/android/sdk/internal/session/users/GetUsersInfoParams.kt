/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.gouv.tchap.android.sdk.internal.session.users

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @property userIds Required. The list of user Matrix IDs to query information about.
 */
@JsonClass(generateAdapter = true)
internal data class GetUsersInfoParams(
        @Json(name = "user_ids") val userIds: List<String>
)
