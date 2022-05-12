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

package im.vector.app.test.fakes

import android.net.Uri
import io.mockk.every
import io.mockk.mockk

class FakeUri(contentEquals: String? = null) {

    val instance = mockk<Uri>()

    init {
        contentEquals?.let {
            givenEquals(it)
            every { instance.toString() } returns it
        }
    }

    fun givenNonHierarchical() {
        givenContent(schema = "mail", path = null)
    }

    fun givenContent(schema: String, path: String?) {
        every { instance.scheme } returns schema
        every { instance.path } returns path
    }

    @Suppress("ReplaceCallWithBinaryOperator")
    fun givenEquals(content: String) {
        every { instance.equals(any()) } answers {
            it.invocation.args.first() == content
        }
        every { instance.hashCode() } answers { content.hashCode() }
    }
}
