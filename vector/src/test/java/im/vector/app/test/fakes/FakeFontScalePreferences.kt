/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.test.fakes

import im.vector.app.R
import im.vector.app.features.settings.FontScalePreferences
import im.vector.app.features.settings.FontScaleValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class FakeFontScalePreferences : FontScalePreferences by mockk(relaxUnitFun = true) {

    private val fontScaleValues = listOf(
            FontScaleValue(0, "FONT_SCALE_TINY", 0.70f, R.string.tiny),
            FontScaleValue(1, "FONT_SCALE_SMALL", 0.85f, R.string.small),
            FontScaleValue(2, "FONT_SCALE_NORMAL", 1.00f, R.string.normal),
            FontScaleValue(3, "FONT_SCALE_LARGE", 1.15f, R.string.large),
            FontScaleValue(4, "FONT_SCALE_LARGER", 1.30f, R.string.larger),
            FontScaleValue(5, "FONT_SCALE_LARGEST", 1.45f, R.string.largest),
            FontScaleValue(6, "FONT_SCALE_HUGE", 1.60f, R.string.huge)
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
