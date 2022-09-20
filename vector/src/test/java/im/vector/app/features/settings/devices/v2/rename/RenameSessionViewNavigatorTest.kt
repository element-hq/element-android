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

package im.vector.app.features.settings.devices.v2.rename

import androidx.fragment.app.FragmentActivity
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class RenameSessionViewNavigatorTest {

    private val renameSessionViewNavigator = RenameSessionViewNavigator()

    @Test
    fun `given an activity when going back then the activity is finished`() {
        // Given
        val fragmentActivity = mockk<FragmentActivity>()
        every { fragmentActivity.finish() } just runs

        // When
        renameSessionViewNavigator.goBack(fragmentActivity)

        // Then
        verify {
            fragmentActivity.finish()
        }
    }
}
