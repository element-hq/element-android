/*
 * Copyright (c) 2024 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.identity

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Sha256Test {
    /**
     * Check that the behavior is the same than what is done in the Olm library.
     * https://gitlab.matrix.org/matrix-org/olm/-/blob/master/tests/test_olm_sha256.cpp#L16
     */
    @Test
    fun testSha256() {
        val sut = Sha256Converter()
        sut.convertToSha256("Hello, World") shouldBeEqualTo "A2daxT/5zRU1zMffzfosRYxSGDcfQY3BNvLRmsH76KU"
    }
}
