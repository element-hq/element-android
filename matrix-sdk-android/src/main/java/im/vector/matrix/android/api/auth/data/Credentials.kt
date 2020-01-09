/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.util.md5

/**
 * This data class hold credentials user data.
 * You shouldn't have to instantiate it.
 * The access token should be use to authenticate user in all server requests.
 */
@JsonClass(generateAdapter = true)
data class Credentials(
        @Json(name = "user_id") val userId: String,
        @Json(name = "home_server") val homeServer: String,
        @Json(name = "access_token") val accessToken: String,
        @Json(name = "refresh_token") val refreshToken: String?,
        @Json(name = "device_id") val deviceId: String?,
        // Optional data that may contain info to override home server and/or identity server
        @Json(name = "well_known") val wellKnown: WellKnown? = null
)

internal fun Credentials.sessionId(): String {
    return (if (deviceId.isNullOrBlank()) userId else "$userId|$deviceId").md5()
}
