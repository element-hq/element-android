/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.font

import com.airbnb.mvrx.MavericksState
import im.vector.app.features.settings.FontScaleValue

data class FontScaleSettingViewState(
        val availableScaleOptions: List<FontScaleValue> = emptyList(),
        val persistedSettingIndex: Int = 0,
        val useSystemSettings: Boolean = true,
) : MavericksState
