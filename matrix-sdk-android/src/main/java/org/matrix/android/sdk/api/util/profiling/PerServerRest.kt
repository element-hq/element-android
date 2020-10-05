/*
 * Copyright (c) 2020 New Vector Ltd
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

package org.matrix.android.sdk.api.util.profiling

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

interface ProfileResult {
    val name: String?
    val depth: Int?
    val nestedResults: List<ProfileResult>?
}

@JsonClass(generateAdapter = true)
data class SingleProfileResultRest(
        override val name: String? = null,
        override val depth: Int? = 0,
        override val nestedResults: List<SingleProfileResultRest>? = null,
        val startTime: Long? = 0L,
        val execTime: Long? = 0L
) : ProfileResult

@JsonClass(generateAdapter = true)
data class ProfileReport(
        @Json(name = "user")
        val user: String? = null,
        @Json(name = "device")
        val device: String? = null,
        @Json(name = "id")
        val id: String? = null,
        @Json(name = "rootProfileResults")
        val rootProfileResult: SingleProfileResultRest? = null,
        @Json(name = "tag")
        val tag: String? = null
)
