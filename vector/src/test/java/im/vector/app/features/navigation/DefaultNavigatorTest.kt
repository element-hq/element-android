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

package im.vector.app.features.navigation

import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeAnalyticsTracker
import im.vector.app.test.fakes.FakeContext
import im.vector.app.test.fakes.FakeDebugNavigator
import im.vector.app.test.fakes.FakeSpaceStateHandler
import im.vector.app.test.fakes.FakeSupportedVerificationMethodsProvider
import im.vector.app.test.fakes.FakeVectorFeatures
import im.vector.app.test.fakes.FakeVectorPreferences
import im.vector.app.test.fakes.FakeWidgetArgsBuilder
import im.vector.app.test.fixtures.RoomSummaryFixture.aRoomSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.Test

internal class DefaultNavigatorTest {

    private val sessionHolder = FakeActiveSessionHolder()
    private val vectorPreferences = FakeVectorPreferences()
    private val widgetArgsBuilder = FakeWidgetArgsBuilder()
    private val spaceStateHandler = FakeSpaceStateHandler()
    private val supportedVerificationMethodsProvider = FakeSupportedVerificationMethodsProvider()
    private val features = FakeVectorFeatures()
    private val analyticsTracker = FakeAnalyticsTracker()
    private val debugNavigator = FakeDebugNavigator()
    private val coroutineScope = CoroutineScope(SupervisorJob())

    private val navigator = DefaultNavigator(
            sessionHolder.instance,
            vectorPreferences.instance,
            widgetArgsBuilder.instance,
            spaceStateHandler,
            supportedVerificationMethodsProvider.instance,
            features,
            coroutineScope,
            analyticsTracker,
            debugNavigator,
    )

    /**
     * The below test is by no means all that we want to test in [DefaultNavigator].
     * Please add relevant tests as you make changes to or related to other functions in the class.
     */

    @Test
    fun `when switchToSpace, then current space set`() {
        val spaceId = "space-id"
        val spaceSummary = aRoomSummary(spaceId)
        sessionHolder.fakeSession.fakeRoomService.getRoomSummaryReturns(spaceSummary)

        navigator.switchToSpace(FakeContext().instance, spaceId, Navigator.PostSwitchSpaceAction.None)

        spaceStateHandler.verifySetCurrentSpace(spaceId)
    }
}
