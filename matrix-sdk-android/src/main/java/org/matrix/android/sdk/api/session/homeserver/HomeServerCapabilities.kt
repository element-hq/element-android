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
         * True if it is possible to change the display name of the account.
         */
        val canChangeDisplayName: Boolean = true,
        /**
         * True if it is possible to change the avatar of the account.
         */
        val canChangeAvatar: Boolean = true,
        /**
         * True if it is possible to change the 3pid associations of the account.
         */
        val canChange3pid: Boolean = true,
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
        val roomVersions: RoomVersionCapabilities? = null,
        /**
         * True if the home server support threading
         */
        var canUseThreading: Boolean = false
) {

    enum class RoomCapabilitySupport {
        SUPPORTED,
        SUPPORTED_UNSTABLE,
        UNSUPPORTED,
        UNKNOWN
    }

    /**
     * Check if a feature is supported by the homeserver.
     * @return
     *  UNKNOWN if the server does not implement room caps
     *  UNSUPPORTED if this feature is not supported
     *  SUPPORTED if this feature is supported by a stable version
     *  SUPPORTED_UNSTABLE if this feature is supported by an unstable version
     *  (unstable version should only be used for dev/experimental purpose)
     */
    fun isFeatureSupported(feature: String): RoomCapabilitySupport {
        if (roomVersions?.capabilities == null) return RoomCapabilitySupport.UNKNOWN
        val info = roomVersions.capabilities[feature] ?: return RoomCapabilitySupport.UNSUPPORTED

        val preferred = info.preferred ?: info.support.lastOrNull()
        val versionCap = roomVersions.supportedVersion.firstOrNull { it.version == preferred }

        return when {
            versionCap == null                            -> {
                RoomCapabilitySupport.UNKNOWN
            }
            versionCap.status == RoomVersionStatus.STABLE -> {
                RoomCapabilitySupport.SUPPORTED
            }
            else                                          -> {
                RoomCapabilitySupport.SUPPORTED_UNSTABLE
            }
        }
    }

    fun isFeatureSupported(feature: String, byRoomVersion: String): Boolean {
        if (roomVersions?.capabilities == null) return false
        val info = roomVersions.capabilities[feature] ?: return false

        return info.preferred == byRoomVersion || info.support.contains(byRoomVersion)
    }

    /**
     * Use this method to know if you should force a version when creating
     * a room that requires this feature.
     * You can also use #isFeatureSupported prior to this call to check if the
     * feature is supported and report some feedback to user.
     */
    fun versionOverrideForFeature(feature: String): String? {
        val cap = roomVersions?.capabilities?.get(feature)
        return cap?.preferred ?: cap?.support?.lastOrNull()
    }

    companion object {
        const val MAX_UPLOAD_FILE_SIZE_UNKNOWN = -1L
        const val ROOM_CAP_KNOCK = "knock"
        const val ROOM_CAP_RESTRICTED = "restricted"
    }
}
