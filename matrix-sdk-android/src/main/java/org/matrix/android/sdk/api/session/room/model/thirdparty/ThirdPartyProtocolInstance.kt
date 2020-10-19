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

package org.matrix.android.sdk.api.session.room.model.thirdparty

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ThirdPartyProtocolInstance(
        /**
         * Required. A human-readable description for the protocol, such as the name.
         */
        @Json(name = "desc")
        val desc: String? = null,

        /**
         * An optional content URI representing the protocol. Overrides the one provided at the higher level Protocol object.
         */
        @Json(name = "icon")
        val icon: String? = null,

        /**
         * Required. Preset values for fields the client may use to search by.
         */
        @Json(name = "fields")
        val fields: Map<String, Any>? = null,

        /**
         * Required. A unique identifier across all instances.
         */
        @Json(name = "network_id")
        val networkId: String? = null,

        /**
         * FIXDOC Not documented on matrix.org doc
         */
        @Json(name = "instance_id")
        val instanceId: String? = null,

        /**
         * FIXDOC Not documented on matrix.org doc
         */
        @Json(name = "bot_user_id")
        val botUserId: String? = null
)
