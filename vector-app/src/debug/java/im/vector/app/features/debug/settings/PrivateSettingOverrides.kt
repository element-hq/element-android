/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.settings

sealed interface BooleanHomeserverCapabilitiesOverride : OverrideOption {

    companion object {
        fun from(value: Boolean?) = when (value) {
            null -> null
            true -> ForceEnabled
            false -> ForceDisabled
        }
    }

    object ForceEnabled : BooleanHomeserverCapabilitiesOverride {
        override val label = "Force enabled"
    }

    object ForceDisabled : BooleanHomeserverCapabilitiesOverride {
        override val label = "Force disabled"
    }
}

fun BooleanHomeserverCapabilitiesOverride?.toBoolean() = when (this) {
    null -> null
    BooleanHomeserverCapabilitiesOverride.ForceDisabled -> false
    BooleanHomeserverCapabilitiesOverride.ForceEnabled -> true
}
