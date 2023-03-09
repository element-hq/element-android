/*
 * Copyright (c) 2023 New Vector Ltd
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
