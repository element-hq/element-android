/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
        val mapTileServerConfig: MapTileServerConfig? = null,

        /**
         * Migration banner to Element X.
         * Phase 1: Opt in, so if data is not provided, the banner will not be displayed.
         * Phase 2: Opt out, so if data is not provided, the banner will be displayed, except if
         * [MigrationBanner.enabled] is set to false.
         */
        @Json(name = "io.element.migration_banner")
        val migrationBanner: MigrationBanner? = null,
) {
    fun getBestMapTileServerConfig() = mapTileServerConfig ?: unstableMapTileServerConfig
}

@JsonClass(generateAdapter = true)
data class MigrationBanner(
        // If true or not provided, the migration banner will be displayed. If false, it will not be displayed.
        @Json(name = "enabled")
        val enabled: Boolean? = true,

        // If null or empty, will default to "Download Element X"
        @Json(name = "title")
        val title: String? = null,

        // If null or empty, will default to "Faster sync, a cleaner design, and new features
        // you won't find here. The Element Classic app will be retired soon, so now's the time to make the move."
        @Json(name = "body")
        val body: String? = null,

        // Label of the download button. If null or blank, will default to "Download app".
        @Json(name = "button_text")
        val buttonText: String? = null,

        // Application id of the app the download button points to.
        // If null, will default to "io.element.android.x" (the Element X FOSS app).
        // If blank, no button will be displayed.
        @Json(name = "target_app_id_android")
        val targetAppIdAndroid: String? = null,
)

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
