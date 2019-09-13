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

package im.vector.matrix.android.internal.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * An interactive authentication flow.
 */
@JsonClass(generateAdapter = true)
data class InteractiveAuthenticationFlow(

        @Json(name = "type")
        val type: String? = null,

        @Json(name = "stages")
        val stages: List<String>? = null
) {

    companion object {
        // Possible values for type
        const val TYPE_LOGIN_SSO = "m.login.sso"
        const val TYPE_LOGIN_TOKEN = "m.login.token"
        const val TYPE_LOGIN_PASSWORD = "m.login.password"
    }
}