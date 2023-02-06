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

package org.matrix.android.sdk.api.session.room.model

import org.amshove.kluent.shouldBe
import org.junit.Test

internal class VersioningStateTest {

    @Test
    fun `when VersioningState is NONE, then isUpgraded returns false`() {
        val versioningState = VersioningState.NONE

        val isUpgraded = versioningState.isUpgraded()

        isUpgraded shouldBe false
    }

    @Test
    fun `when VersioningState is UPGRADED_ROOM_NOT_JOINED, then isUpgraded returns true`() {
        val versioningState = VersioningState.UPGRADED_ROOM_NOT_JOINED

        val isUpgraded = versioningState.isUpgraded()

        isUpgraded shouldBe true
    }

    @Test
    fun `when VersioningState is UPGRADED_ROOM_JOINED, then isUpgraded returns true`() {
        val versioningState = VersioningState.UPGRADED_ROOM_JOINED

        val isUpgraded = versioningState.isUpgraded()

        isUpgraded shouldBe true
    }
}
