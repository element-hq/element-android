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
package org.matrix.android.sdk.internal.session.profile

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class DeleteThreePidResponse(
        /**
         * Required. An indicator as to whether or not the homeserver was able to unbind the 3PID from
         * the identity server. success indicates that the identity server has unbound the identifier
         * whereas no-support indicates that the identity server refuses to support the request or the
         * homeserver was not able to determine an identity server to unbind from. One of: ["no-support", "success"]
         */
        @Json(name = "id_server_unbind_result")
        val idServerUnbindResult: String? = null
)
