/*
 * Copyright 2019 New Vector Ltd
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

/**
 * This class describes a rooms directory server.
 */
data class RoomDirectoryData(

        /**
         * The server name (might be null)
         * Set null when the server is the current user's home server.
         */
        val homeServer: String? = null,

        /**
         * The display name (the server description)
         */
        val displayName: String = DEFAULT_HOME_SERVER_NAME,

        /**
         * The third party server identifier
         */
        val thirdPartyInstanceId: String? = null,

        /**
         * Tell if all the federated servers must be included
         */
        val includeAllNetworks: Boolean = false,

        /**
         * the avatar url
         */
        val avatarUrl: String? = null
) {

    companion object {
        const val DEFAULT_HOME_SERVER_NAME = "Matrix"
    }
}
