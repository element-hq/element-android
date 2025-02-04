/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.features.settings.FontScalePreferences
import im.vector.app.features.settings.FontScaleValue
import im.vector.lib.strings.CommonStrings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class FakeFontScalePreferences : FontScalePreferences by mockk(relaxUnitFun = true) {

    private val fontScaleValues = listOf(
            FontScaleValue(0, "FONT_SCALE_TINY", 0.70f, CommonStrings.tiny),
            FontScaleValue(1, "FONT_SCALE_SMALL", 0.85f, CommonStrings.small),
            FontScaleValue(2, "FONT_SCALE_NORMAL", 1.00f, CommonStrings.normal),
            FontScaleValue(3, "FONT_SCALE_LARGE", 1.15f, CommonStrings.large),
            FontScaleValue(4, "FONT_SCALE_LARGER", 1.30f, CommonStrings.larger),
            FontScaleValue(5, "FONT_SCALE_LARGEST", 1.45f, CommonStrings.largest),
            FontScaleValue(6, "FONT_SCALE_HUGE", 1.60f, CommonStrings.huge)
    )

    init {
        every { getAvailableScales() } returns fontScaleValues
        every { getUseSystemScale() } returns true
        every { getAppFontScaleValue() } returns fontScaleValues[0]
        every { getResolvedFontScaleValue() } returns fontScaleValues[0]
    }

    fun givenAppSettingIsDifferentFromSystemSetting() {
        every { getResolvedFontScaleValue() } returns fontScaleValues[2] andThen fontScaleValues[0]
    }

    fun verifyAppScaleFontValue(value: FontScaleValue) {
        verify {
            setFontScaleValue(value)
        }
    }

    fun givenAvailableScaleOptions(availableFontScales: List<FontScaleValue>) {
        every { getAvailableScales() } returns availableFontScales
    }
}
