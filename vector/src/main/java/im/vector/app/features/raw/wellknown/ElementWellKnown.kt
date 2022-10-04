/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.raw.wellknown

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ElementWellKnown(
        /**
         * Preferred Jitsi domain.
         */
        @Json(name = "im.vector.riot.jitsi")
        val jitsiServer: WellKnownPreferredConfig? = null,

        /**
         * The settings above were first proposed under a im.vector.riot.e2ee key, which is now deprecated.
         * Element will check for either key, preferring io.element.e2ee if both exist.
         */
        @Json(name = "io.element.e2ee")
        val elementE2E: E2EWellKnownConfig? = null,

        @Json(name = "im.vector.riot.e2ee")
        val riotE2E: E2EWellKnownConfig? = null,

        @Json(name = "org.matrix.msc3488.tile_server")
        val unstableMapTileServerConfig: MapTileServerConfig? = null,

        @Json(name = "m.tile_server")
        val mapTileServerConfig: MapTileServerConfig? = null
) {
    fun getBestMapTileServerConfig() = mapTileServerConfig ?: unstableMapTileServerConfig
}

@JsonClass(generateAdapter = true)
data class E2EWellKnownConfig(
        /**
         * Option to allow homeserver admins to set the default E2EE behaviour back to disabled for DMs / private rooms
         * (as it was before) for various environments where this is desired.
         */
        @Json(name = "default")
        val e2eDefault: Boolean? = null,

        @Json(name = "secure_backup_required")
        val secureBackupRequired: Boolean? = null,

        /**
         * The new field secure_backup_setup_methods is an array listing the methods the client should display.
         * Supported values currently include key and passphrase.
         * If the secure_backup_setup_methods field is not present or exists but does not contain any supported methods,
         * clients should fallback to the default value of: ["key", "passphrase"].
         */
        @Json(name = "secure_backup_setup_methods")
        val secureBackupSetupMethods: List<String>? = null,

        /**
         * Configuration for sharing keys strategy which should be used instead of [im.vector.app.config.Config.KEY_SHARING_STRATEGY].
         * One of on_room_opening, on_typing or disabled.
         */
        @Json(name = "outbound_keys_pre_sharing_mode")
        val outboundsKeyPreSharingMode: String? = null,
)

@JsonClass(generateAdapter = true)
data class WellKnownPreferredConfig(
        @Json(name = "preferredDomain")
        val preferredDomain: String? = null
)

@JsonClass(generateAdapter = true)
data class MapTileServerConfig(
        @Json(name = "map_style_url")
        val mapStyleUrl: String? = null
)
