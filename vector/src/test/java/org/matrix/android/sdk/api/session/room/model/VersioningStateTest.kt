/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
