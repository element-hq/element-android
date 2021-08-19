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

package org.matrix.client.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Model for https://matrix.org/docs/spec/client_server/latest#get-matrix-client-versions
 *
 * Ex:
 * <pre>
 *   {
 *     "unstable_features": {
 *       "m.lazy_load_members": true
 *     },
 *     "versions": [
 *       "r0.0.1",
 *       "r0.1.0",
 *       "r0.2.0",
 *       "r0.3.0"
 *     ]
 *   }
 * </pre>
 */
@JsonClass(generateAdapter = true)
data class Versions(
        @Json(name = "versions")
        val supportedVersions: List<String>? = null,

        @Json(name = "unstable_features")
        val unstableFeatures: Map<String, Boolean>? = null
)
