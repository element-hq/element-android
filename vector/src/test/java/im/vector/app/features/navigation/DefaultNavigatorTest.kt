/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
