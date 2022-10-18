/*
 * Copyright (c) 2021 New Vector Ltd
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

import im.vector.app.features.settings.VectorPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class FakeVectorPreferences {

    val instance = mockk<VectorPreferences>(relaxUnitFun = true)

    fun givenUseCompleteNotificationFormat(value: Boolean) {
        every { instance.useCompleteNotificationFormat() } returns value
    }

    fun givenSpaceBackstack(value: List<String?>) {
        every { instance.getSpaceBackstack() } returns value
    }

    fun verifySetSpaceBackstack(value: List<String?>, inverse: Boolean = false) {
        verify(inverse = inverse) { instance.setSpaceBackstack(value) }
    }

    fun givenIsClientInfoRecordingEnabled(isEnabled: Boolean) {
        every { instance.isClientInfoRecordingEnabled() } returns isEnabled
    }
}
