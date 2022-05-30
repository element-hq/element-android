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

package im.vector.app.core.extensions

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class StringExtensionsTest {

    @Test
    fun `given text with RtL unicode override, when checking contains RtL Override, then returns true`() {
        val textWithRtlOverride = "hello\u202Eworld"

        val result = textWithRtlOverride.containsRtLOverride()

        result shouldBeEqualTo true
    }

    @Test
    fun `given text without RtL unicode override, when checking contains RtL Override, then returns false`() {
        val textWithRtlOverride = "hello world"

        val result = textWithRtlOverride.containsRtLOverride()

        result shouldBeEqualTo false
    }

    @Test
    fun `given text with RtL unicode override, when ensuring ends LtR, then appends a LtR unicode override`() {
        val textWithRtlOverride = "123\u202E456"

        val result = textWithRtlOverride.ensureEndsLeftToRight()

        result shouldBeEqualTo "$textWithRtlOverride\u202D"
    }

    @Test
    fun `given text with unicode direction overrides, when filtering direction overrides, then removes all overrides`() {
        val textWithDirectionOverrides = "123\u202E456\u202d789"

        val result = textWithDirectionOverrides.filterDirectionOverrides()

        result shouldBeEqualTo "123456789"
    }
}
