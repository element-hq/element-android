/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.session.homeserver

data class HomeServerCapabilities(
        /**
         * True if it is possible to change the password of the account.
         */
        val canChangePassword: Boolean = true,
        /**
         * Max size of file which can be uploaded to the homeserver in bytes. [MAX_UPLOAD_FILE_SIZE_UNKNOWN] if unknown or not retrieved yet
         */
        val maxUploadFileSize: Long = MAX_UPLOAD_FILE_SIZE_UNKNOWN,
        /**
         * Last version identity server and binding supported
         */
        val lastVersionIdentityServerSupported: Boolean = false,
        /**
         * Default identity server url, provided in Wellknown
         */
        val defaultIdentityServerUrl: String? = null,
        /**
         * Room versions supported by the server
         * This capability describes the default and available room versions a server supports, and at what level of stability.
         * Clients should make use of this capability to determine if users need to be encouraged to upgrade their rooms.
         */
        val roomVersions: RoomVersionCapabilities? = null
) {
    companion object {
        const val MAX_UPLOAD_FILE_SIZE_UNKNOWN = -1L
    }
}
