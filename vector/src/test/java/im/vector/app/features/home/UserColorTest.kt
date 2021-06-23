/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.home

import im.vector.app.R
import im.vector.app.features.home.room.detail.timeline.helper.MatrixItemColorProvider.Companion.getColorFromUserId
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
