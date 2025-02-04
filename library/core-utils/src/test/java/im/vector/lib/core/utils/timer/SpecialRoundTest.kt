/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.core.utils.timer

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class SpecialRoundTest {
    @Test
    fun `test special round 500`() {
        val sut = SpecialRound(500)
        sut.round(1) shouldBeEqualTo 0
        sut.round(499) shouldBeEqualTo 500
        sut.round(500) shouldBeEqualTo 500
        sut.round(501) shouldBeEqualTo 500
        sut.round(999) shouldBeEqualTo 1_000
        sut.round(1000) shouldBeEqualTo 1_000
        sut.round(1001) shouldBeEqualTo 1_000
        sut.round(1499) shouldBeEqualTo 1_500
        sut.round(1500) shouldBeEqualTo 1_500
        sut.round(1501) shouldBeEqualTo 1_500
    }

    @Test
    fun `test special round 1_000`() {
        val sut = SpecialRound(1_000)
        sut.round(1) shouldBeEqualTo 0
        sut.round(499) shouldBeEqualTo 0
        sut.round(500) shouldBeEqualTo 0
        sut.round(501) shouldBeEqualTo 1_000
        sut.round(999) shouldBeEqualTo 1_000
        sut.round(1000) shouldBeEqualTo 1_000
        sut.round(1001) shouldBeEqualTo 1_000
        sut.round(1499) shouldBeEqualTo 1_000
        sut.round(1500) shouldBeEqualTo 2_000
        sut.round(1501) shouldBeEqualTo 2_000
    }
}
