/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes

/**
 * This class provides the authentication data by using user and password
 */
@JsonClass(generateAdapter = true)
data class UserPasswordAuth(

        // device device session id
        @Json(name = "session")
        val session: String? = null,

        // registration information
        @Json(name = "type")
        val type: String? = LoginFlowTypes.PASSWORD,

        @Json(name = "user")
        val user: String? = null,

        @Json(name = "password")
        val password: String? = null
)
