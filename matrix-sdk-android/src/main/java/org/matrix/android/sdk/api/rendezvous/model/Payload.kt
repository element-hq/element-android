/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.rendezvous.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class Payload(
        val type: PayloadType,
        val intent: RendezvousIntent? = null,
        val outcome: Outcome? = null,
        val protocols: List<Protocol>? = null,
        val protocol: Protocol? = null,
        val homeserver: String? = null,
        @Json(name = "login_token") val loginToken: String? = null,
        @Json(name = "device_id") val deviceId: String? = null,
        @Json(name = "device_key") val deviceKey: String? = null,
        @Json(name = "verifying_device_id") val verifyingDeviceId: String? = null,
        @Json(name = "verifying_device_key") val verifyingDeviceKey: String? = null,
        @Json(name = "master_key") val masterKey: String? = null
)
