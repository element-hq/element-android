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
package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This class provides the parameter to delete several devices.
 */
@JsonClass(generateAdapter = true)
internal data class DeleteDevicesParams(
        /**
         * Additional authentication information for the user-interactive authentication API.
         */
        @Json(name = "auth")
        val auth: Map<String, *>? = null,

        /**
         * Required: The list of device IDs to delete.
         */
        @Json(name = "devices")
        val deviceIds: List<String>,
)
