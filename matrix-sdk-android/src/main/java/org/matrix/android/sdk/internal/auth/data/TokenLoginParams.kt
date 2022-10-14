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

package org.matrix.android.sdk.internal.auth.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes

@JsonClass(generateAdapter = true)
internal data class TokenLoginParams(
        @Json(name = "type") override val type: String = LoginFlowTypes.TOKEN,
        @Json(name = "token") val token: String,
        @Json(name = "initial_device_display_name") override val deviceDisplayName: String? = null,
        @Json(name = "device_id") override val deviceId: String? = null
) : LoginParams
