/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.elementx

import im.vector.app.core.resources.StringProvider
import im.vector.app.features.raw.wellknown.ElementWellKnown
import im.vector.app.features.raw.wellknown.MigrationBanner
import im.vector.lib.strings.R

/**
 * Application id of the Element X FOSS app, used when the homeserver does not specify a target.
 */
const val DEFAULT_TARGET_APP_ID = "io.element.android.x"

sealed interface MigrationBannerState {
    data object Hide : MigrationBannerState
    data class Show(
            val title: String,
            val body: String,
            val showButton: Boolean,
            val buttonText: String,
            val targetAppId: String,
    ) : MigrationBannerState {
        val isElementX: Boolean
            get() = targetAppId == DEFAULT_TARGET_APP_ID
    }
}

fun ElementWellKnown?.toMigrationBannerState(
        stringProvider: StringProvider,
        useDefaultOnMissingData: Boolean,
): MigrationBannerState {
    // No data at all: either the homeserver has no .well-known file, or it has one with no
    // migration banner section. Before the switch date the banner stays hidden, after it we
    // fall back to the default content.
    val migrationBannerConfig = this?.migrationBanner
            ?: MigrationBanner().takeIf { useDefaultOnMissingData }
            ?: return MigrationBannerState.Hide
    if (!migrationBannerConfig.enabled) return MigrationBannerState.Hide

    val title = migrationBannerConfig.title?.takeIf { it.isNotBlank() } ?: stringProvider.getString(R.string.view_element_x_banner_title)
    val body = migrationBannerConfig.body?.takeIf { it.isNotBlank() } ?: stringProvider.getString(R.string.view_element_x_banner_body)
    // A blank target app id means the homeserver explicitly wants no download button.
    val showButton = migrationBannerConfig.targetAppIdAndroid?.isNotBlank() ?: true
    val buttonText = migrationBannerConfig.buttonText?.takeIf { it.isNotBlank() } ?: stringProvider.getString(R.string.view_element_x_banner_button)
    val targetAppId = migrationBannerConfig.targetAppIdAndroid?.takeIf { it.isNotBlank() } ?: DEFAULT_TARGET_APP_ID

    return MigrationBannerState.Show(
            title = title,
            body = body,
            showButton = showButton,
            buttonText = buttonText,
            targetAppId = targetAppId,
    )
}
