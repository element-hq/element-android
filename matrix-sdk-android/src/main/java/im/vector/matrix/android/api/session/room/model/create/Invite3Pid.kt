/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.api.session.room.model.create

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Invite3Pid(
        /**
         * Required.
         * The hostname+port of the identity server which should be used for third party identifier lookups.
         */
        @Json(name = "id_server")
        val idServer: String,

        /**
         * Required.
         * The kind of address being passed in the address field, for example email.
         */
        val medium: String,

        /**
         * Required.
         * The invitee's third party identifier.
         */
        val address: String
)
