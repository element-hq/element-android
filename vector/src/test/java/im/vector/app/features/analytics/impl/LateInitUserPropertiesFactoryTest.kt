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

package im.vector.app.features.analytics.impl

import im.vector.app.features.analytics.plan.UserProperties
import im.vector.app.features.onboarding.FtueUseCase
import im.vector.app.test.fakes.FakeActiveSessionDataSource
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeSession
import im.vector.app.test.fakes.FakeVectorStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

@ExperimentalCoroutinesApi
class LateInitUserPropertiesFactoryTest {

    private val fakeActiveSessionDataSource = FakeActiveSessionDataSource()
    private val fakeVectorStore = FakeVectorStore()
    private val fakeContext = FakeContext()
    private val fakeSession = FakeSession().also {
        it.givenVectorStore(fakeVectorStore.instance)
    }

    private val lateInitUserProperties = LateInitUserPropertiesFactory(
            fakeActiveSessionDataSource.instance,
            fakeContext.instance
    )

    @Test
    fun `given no active session when creating properties then returns null`() = runTest {
        val result = lateInitUserProperties.createUserProperties()

        result shouldBeEqualTo null
    }

    @Test
    fun `given no use case set on an active session when creating properties then returns null`() = runTest {
        fakeVectorStore.givenUseCase(null)
        fakeSession.givenVectorStore(fakeVectorStore.instance)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)

        val result = lateInitUserProperties.createUserProperties()

        result shouldBeEqualTo null
    }

    @Test
    fun `given use case set on an active session when creating properties then includes the use case`() = runTest {
        fakeVectorStore.givenUseCase(FtueUseCase.TEAMS)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)
        val result = lateInitUserProperties.createUserProperties()

        result shouldBeEqualTo UserProperties(
                ftueUseCaseSelection = UserProperties.FtueUseCaseSelection.WorkMessaging
        )
    }
}
