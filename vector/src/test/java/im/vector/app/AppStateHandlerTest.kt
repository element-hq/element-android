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

package im.vector.app

import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.ui.UiStateRepository
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeSession
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.room.model.RoomSummary

internal class AppStateHandlerTest {

    private val spaceId = "spaceId"
    private val spaceSummary: RoomSummary = mockk {
        every { roomId } returns spaceId
    }
    private val session = FakeSession.withRoomSummary(spaceSummary)

    private val sessionDataSource: ActiveSessionDataSource = mockk()
    private val uiStateRepository: UiStateRepository = mockk()
    private val activeSessionHolder = FakeActiveSessionHolder(session).instance
    private val analyticsTracker: AnalyticsTracker = mockk()

    private val appStateHandler = AppStateHandlerImpl(
            sessionDataSource,
            uiStateRepository,
            activeSessionHolder,
            analyticsTracker,
    )

    @Before
    fun setup() {
        justRun { uiStateRepository.storeSelectedSpace(any(), any()) }
    }

    @Test
    fun `given selected space doesn't exist, when getCurrentSpace, then return null`() {
        val currentSpace = appStateHandler.getCurrentSpace()

        currentSpace shouldBe null
    }

    @Test
    fun `given selected space exists, when getCurrentSpace, then return selected space`() {
        appStateHandler.setCurrentSpace(spaceId, session)

        val currentSpace = appStateHandler.getCurrentSpace()

        currentSpace shouldBe spaceSummary
    }
}
