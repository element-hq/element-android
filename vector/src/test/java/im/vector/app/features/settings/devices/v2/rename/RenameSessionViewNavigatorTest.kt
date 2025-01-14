/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
