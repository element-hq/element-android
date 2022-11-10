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

import androidx.fragment.app.FragmentActivity
import im.vector.app.core.pushers.PushersManager
import im.vector.app.core.pushers.UnifiedPushHelper
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class FakeUnifiedPushHelper {

    val instance = mockk<UnifiedPushHelper>()

    fun givenRegister(fragmentActivity: FragmentActivity) {
        every { instance.register(fragmentActivity, any()) } answers {
            secondArg<Runnable>().run()
        }
    }

    fun verifyRegister(fragmentActivity: FragmentActivity) {
        verify { instance.register(fragmentActivity, any()) }
    }

    fun givenUnregister(pushersManager: PushersManager) {
        coJustRun { instance.unregister(pushersManager) }
    }

    fun verifyUnregister(pushersManager: PushersManager) {
        coVerify { instance.unregister(pushersManager) }
    }

    fun givenIsEmbeddedDistributorReturns(isEmbedded: Boolean) {
        every { instance.isEmbeddedDistributor() } returns isEmbedded
    }
}
