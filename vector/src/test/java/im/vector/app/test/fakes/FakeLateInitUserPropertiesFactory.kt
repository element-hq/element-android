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

import im.vector.app.features.analytics.impl.LateInitUserPropertiesFactory
import im.vector.app.features.analytics.plan.UserProperties
import io.mockk.coEvery
import io.mockk.mockk

class FakeLateInitUserPropertiesFactory {

    val instance = mockk<LateInitUserPropertiesFactory>()

    fun givenCreatesProperties(userProperties: UserProperties?) {
        coEvery { instance.createUserProperties() } returns userProperties
    }
}
