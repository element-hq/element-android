/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.settings

import com.airbnb.mvrx.MavericksState
import im.vector.app.features.debug.settings.OverrideDropdownView.OverrideDropdown

data class DebugPrivateSettingsViewState(
        val dialPadVisible: Boolean = false,
        val forceLoginFallback: Boolean = false,
        val homeserverCapabilityOverrides: HomeserverCapabilityOverrides = HomeserverCapabilityOverrides(),
        val releaseNotesActivityHasBeenDisplayed: Boolean = false,
) : MavericksState

data class HomeserverCapabilityOverrides(
        val displayName: OverrideDropdown<BooleanHomeserverCapabilitiesOverride> = OverrideDropdown(
                label = "Override display name capability",
                activeOption = null,
                options = listOf(BooleanHomeserverCapabilitiesOverride.ForceEnabled, BooleanHomeserverCapabilitiesOverride.ForceDisabled)
        ),
        val avatar: OverrideDropdown<BooleanHomeserverCapabilitiesOverride> = OverrideDropdown(
                label = "Override avatar capability",
                activeOption = null,
                options = listOf(BooleanHomeserverCapabilitiesOverride.ForceEnabled, BooleanHomeserverCapabilitiesOverride.ForceDisabled)
        )
)
