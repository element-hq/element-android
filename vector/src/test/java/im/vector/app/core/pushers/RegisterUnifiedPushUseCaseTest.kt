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

package im.vector.app.core.pushers

import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeVectorFeatures
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyAll
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.unifiedpush.android.connector.UnifiedPush

class RegisterUnifiedPushUseCaseTest {

    private val fakeContext = FakeContext()
    private val fakeVectorFeatures = FakeVectorFeatures()

    private val registerUnifiedPushUseCase = RegisterUnifiedPushUseCase(
            context = fakeContext.instance,
            vectorFeatures = fakeVectorFeatures,
    )

    @Before
    fun setup() {
        mockkStatic(UnifiedPush::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given non empty distributor when execute then distributor is saved and app is registered`() = runTest {
        // Given
        val aDistributor = "distributor"
        justRun { UnifiedPush.registerApp(any()) }
        justRun { UnifiedPush.saveDistributor(any(), any()) }

        // When
        val result = registerUnifiedPushUseCase.execute(aDistributor)

        // Then
        result shouldBe RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.Success
        verifyOrder {
            UnifiedPush.saveDistributor(fakeContext.instance, aDistributor)
            UnifiedPush.registerApp(fakeContext.instance)
        }
    }

    @Test
    fun `given external distributors are not allowed when execute then internal distributor is saved and app is registered`() = runTest {
        // Given
        val aPackageName = "packageName"
        fakeContext.givenPackageName(aPackageName)
        justRun { UnifiedPush.registerApp(any()) }
        justRun { UnifiedPush.saveDistributor(any(), any()) }
        fakeVectorFeatures.givenExternalDistributorsAreAllowed(false)

        // When
        val result = registerUnifiedPushUseCase.execute()

        // Then
        result shouldBe RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.Success
        verifyOrder {
            UnifiedPush.saveDistributor(fakeContext.instance, aPackageName)
            UnifiedPush.registerApp(fakeContext.instance)
        }
    }

    @Test
    fun `given a saved distributor and external distributors are allowed when execute then app is registered`() = runTest {
        // Given
        justRun { UnifiedPush.registerApp(any()) }
        val aDistributor = "distributor"
        every { UnifiedPush.getDistributor(any()) } returns aDistributor
        fakeVectorFeatures.givenExternalDistributorsAreAllowed(true)

        // When
        val result = registerUnifiedPushUseCase.execute()

        // Then
        result shouldBe RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.Success
        verifyAll {
            UnifiedPush.getDistributor(fakeContext.instance)
            UnifiedPush.registerApp(fakeContext.instance)
        }
    }

    @Test
    fun `given no saved distributor and a unique distributor available when execute then the distributor is saved and app is registered`() = runTest {
        // Given
        justRun { UnifiedPush.registerApp(any()) }
        justRun { UnifiedPush.saveDistributor(any(), any()) }
        every { UnifiedPush.getDistributor(any()) } returns ""
        fakeVectorFeatures.givenExternalDistributorsAreAllowed(true)
        val aDistributor = "distributor"
        every { UnifiedPush.getDistributors(any()) } returns listOf(aDistributor)

        // When
        val result = registerUnifiedPushUseCase.execute()

        // Then
        result shouldBe RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.Success
        verifyOrder {
            UnifiedPush.getDistributor(fakeContext.instance)
            UnifiedPush.getDistributors(fakeContext.instance)
            UnifiedPush.saveDistributor(fakeContext.instance, aDistributor)
            UnifiedPush.registerApp(fakeContext.instance)
        }
    }

    @Test
    fun `given no saved distributor and multiple distributors available when execute then result is to ask user`() = runTest {
        // Given
        every { UnifiedPush.getDistributor(any()) } returns ""
        fakeVectorFeatures.givenExternalDistributorsAreAllowed(true)
        val aDistributor1 = "distributor1"
        val aDistributor2 = "distributor2"
        every { UnifiedPush.getDistributors(any()) } returns listOf(aDistributor1, aDistributor2)

        // When
        val result = registerUnifiedPushUseCase.execute()

        // Then
        result shouldBe RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.NeedToAskUserForDistributor
        verifyOrder {
            UnifiedPush.getDistributor(fakeContext.instance)
            UnifiedPush.getDistributors(fakeContext.instance)
        }
        verify(inverse = true) {
            UnifiedPush.saveDistributor(any(), any())
            UnifiedPush.registerApp(any())
        }
    }
}
