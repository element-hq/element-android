/*
 * Copyright (c) 2022 BWI GmbH
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

import im.vector.app.features.raw.wellknown.WellknownService
import io.mockk.every
import io.mockk.mockk

class FakeWellknownService {
    val A_MAPSTYLE_URL = "https://example.com/style.json"

    val instance = mockk<WellknownService>()

    fun givenMissingMapConfiguration() {
        every { instance.getMapStyleUrl() } returns null
    }

    fun givenValidMapConfiguration() {
        every { instance.getMapStyleUrl() } returns A_MAPSTYLE_URL
    }
}
