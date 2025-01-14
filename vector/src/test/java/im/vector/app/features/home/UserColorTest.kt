/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import im.vector.app.features.home.room.detail.timeline.helper.MatrixItemColorProvider.Companion.getColorFromUserId
import im.vector.lib.ui.styles.R
import org.junit.Assert.assertEquals
import org.junit.Test

class UserColorTest {

    @Test
    fun testNull() {
        assertEquals(R.color.element_name_01, getColorFromUserId(null))
    }

    @Test
    fun testEmpty() {
        assertEquals(R.color.element_name_01, getColorFromUserId(""))
    }

    @Test
    fun testName() {
        assertEquals(R.color.element_name_01, getColorFromUserId("@ganfra:matrix.org"))
        assertEquals(R.color.element_name_04, getColorFromUserId("@benoit0816:matrix.org"))
        assertEquals(R.color.element_name_05, getColorFromUserId("@hubert:uhoreg.ca"))
        assertEquals(R.color.element_name_07, getColorFromUserId("@nadonomy:matrix.org"))
    }
}
