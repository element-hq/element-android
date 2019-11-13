/*
 * Copyright 2018 New Vector Ltd
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
package im.vector.matrix.android.internal.auth.registration

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.internal.auth.data.LoginFlowTypes

/**
 * Class to define the authentication parameters for "m.login.recaptcha" type
 */
@JsonClass(generateAdapter = true)
class AuthParamsCaptcha(session: String,

                        @Json(name = "response")
                        val response: String)
    : AuthParams(LoginFlowTypes.RECAPTCHA, session)
