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

package im.vector.app.features.onboarding

import android.provider.OpenableColumns
import im.vector.app.test.fakes.FakeContentResolver
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeCursor
import im.vector.app.test.fakes.FakeUri
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val A_LAST_SEGMENT = "a-file-name.foo"
private const val A_DISPLAY_NAME = "file-display-name.foo"

class UriFilenameResolverTest {

    private val fakeUri = FakeUri()
    private val fakeContentResolver = FakeContentResolver()
    private val uriFilenameResolver = UriFilenameResolver(FakeContext(fakeContentResolver.instance).instance)

    @Test
    fun `given a non hierarchical Uri when querying file name then is null`() {
        fakeUri.givenNonHierarchical()

        val result = uriFilenameResolver.getFilenameFromUri(fakeUri.instance)

        result shouldBeEqualTo null
    }

    @Test
    fun `given a non content schema Uri when querying file name then returns last segment`() {
        fakeUri.givenContent(schema = "file", path = "path/to/$A_LAST_SEGMENT")

        val result = uriFilenameResolver.getFilenameFromUri(fakeUri.instance)

        result shouldBeEqualTo A_LAST_SEGMENT
    }

    @Test
    fun `given content schema Uri with no backing content when querying file name then returns last segment`() {
        fakeUri.givenContent(schema = "content", path = "path/to/$A_LAST_SEGMENT")
        fakeContentResolver.givenUriResult(fakeUri.instance, null)

        val result = uriFilenameResolver.getFilenameFromUri(fakeUri.instance)

        result shouldBeEqualTo A_LAST_SEGMENT
    }

    @Test
    fun `given content schema Uri with empty backing content when querying file name then returns last segment`() {
        fakeUri.givenContent(schema = "content", path = "path/to/$A_LAST_SEGMENT")
        val emptyCursor = FakeCursor().also { it.givenEmpty() }
        fakeContentResolver.givenUriResult(fakeUri.instance, emptyCursor.instance)

        val result = uriFilenameResolver.getFilenameFromUri(fakeUri.instance)

        result shouldBeEqualTo A_LAST_SEGMENT
    }

    @Test
    fun `given content schema Uri with backing content when querying file name then returns display name column`() {
        fakeUri.givenContent(schema = "content", path = "path/to/$A_DISPLAY_NAME")
        val aCursor = FakeCursor().also { it.givenString(OpenableColumns.DISPLAY_NAME, A_DISPLAY_NAME) }
        fakeContentResolver.givenUriResult(fakeUri.instance, aCursor.instance)

        val result = uriFilenameResolver.getFilenameFromUri(fakeUri.instance)

        result shouldBeEqualTo A_DISPLAY_NAME
    }
}
