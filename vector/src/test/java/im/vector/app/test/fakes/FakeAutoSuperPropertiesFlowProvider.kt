/*
 * Copyright (c) 2024 New Vector Ltd
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

import im.vector.app.features.analytics.impl.AutoSuperPropertiesFlowProvider
import im.vector.app.features.analytics.plan.SuperProperties
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAutoSuperPropertiesFlowProvider  {

    val flow = MutableSharedFlow<SuperProperties>()

    val instance = mockk<AutoSuperPropertiesFlowProvider>().also {
        every { it.superPropertiesFlow } returns flow
    }

    suspend fun postSuperProperty(properties: SuperProperties) {
        flow.emit(properties)
    }
}
