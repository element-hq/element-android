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

import io.element.android.opusencoder.OggOpusEncoder
import io.mockk.every
import io.mockk.mockk
import java.io.File

class FakeOggOpusEncoder : OggOpusEncoder by mockk() {

    init {
        every { init(any(), any()) } returns 0
        every { setBitrate(any()) } returns 0
        every { encode(any(), any()) } returns 0
        every { release() } answers {}
    }

    fun createEmptyFileOnInit() {
        every { init(any(), any()) } answers {
            val filePath = arg<String>(0)
            if (File(filePath).createNewFile()) 0 else 1
        }
    }
}
