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

import android.content.SharedPreferences
import im.vector.app.features.settings.FontScaleValue
import io.mockk.every
import io.mockk.mockk

class FakeSharedPreferences : SharedPreferences by mockk() {

    fun givenFontScaleIsSetTo(fontScaleValue: FontScaleValue) {
        every { contains("APPLICATION_FONT_SCALE_KEY") } returns true
        every { getString("APPLICATION_FONT_SCALE_KEY", any()) } returns fontScaleValue.preferenceValue
    }

    fun givenUseSystemFontScaleIsSetTo(useSystemScale: Boolean) {
        every { contains("APPLICATION_USE_SYSTEM_FONT_SCALE_KEY") } returns true
        every { getBoolean("APPLICATION_USE_SYSTEM_FONT_SCALE_KEY", any()) } returns useSystemScale
    }
}
