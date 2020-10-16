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

package org.matrix.android.sdk.internal.session.user.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Class representing an user search parameters
 */
@JsonClass(generateAdapter = true)
internal data class SearchUsersParams(
        // the searched term
        @Json(name = "search_term") val searchTerm: String,
        // set a limit to the request response
        @Json(name = "limit") val limit: Int
)
