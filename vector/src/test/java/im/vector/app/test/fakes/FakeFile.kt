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
import androidx.core.net.toUri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.File

class FakeFile {

    val instance = mockk<File>()

    init {
        mockkStatic(Uri::class)
    }

    /**
     * To be called after tests.
     */
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    fun givenName(name: String) {
        every { instance.name } returns name
    }

    fun givenUri(uri: Uri) {
        every { instance.toUri() } returns uri
    }
}
