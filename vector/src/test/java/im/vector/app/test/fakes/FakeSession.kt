/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import im.vector.app.core.extensions.configureAndStart
import im.vector.app.core.extensions.startSyncing
import im.vector.app.core.extensions.vectorStore
import im.vector.app.features.session.VectorSessionStore
import im.vector.app.test.testCoroutineDispatchers
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilitiesService
import org.matrix.android.sdk.api.session.profile.ProfileService
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.flow.FlowSession
import org.matrix.android.sdk.flow.flow

class FakeSession(
        val fakeCryptoService: FakeCryptoService = FakeCryptoService(),
        val fakeProfileService: FakeProfileService = FakeProfileService(),
        val fakeHomeServerCapabilitiesService: FakeHomeServerCapabilitiesService = FakeHomeServerCapabilitiesService(),
        val fakeSharedSecretStorageService: FakeSharedSecretStorageService = FakeSharedSecretStorageService(),
        val fakeRoomService: FakeRoomService = FakeRoomService(),
        private val fakeEventService: FakeEventService = FakeEventService(),
) : Session by mockk(relaxed = true) {

    init {
        mockkStatic("im.vector.app.core.extensions.SessionKt")
    }

    override val myUserId: String = "@fake:server.fake"

    override val coroutineDispatchers = testCoroutineDispatchers

    override fun cryptoService() = fakeCryptoService
    override fun profileService(): ProfileService = fakeProfileService
    override fun homeServerCapabilitiesService(): HomeServerCapabilitiesService = fakeHomeServerCapabilitiesService
    override fun sharedSecretStorageService() = fakeSharedSecretStorageService
    override fun roomService() = fakeRoomService
    override fun eventService() = fakeEventService

    fun givenVectorStore(vectorSessionStore: VectorSessionStore) {
        coEvery {
            this@FakeSession.vectorStore(any())
        } coAnswers {
            vectorSessionStore
        }
    }

    fun expectStartsSyncing() {
        coJustRun {
            this@FakeSession.configureAndStart(any(), startSyncing = true)
            this@FakeSession.startSyncing(any())
        }
    }

    fun givenSessionParams(sessionParams: SessionParams) {
        every { this@FakeSession.sessionParams } returns sessionParams
    }

    /**
     * Do not forget to call mockkStatic("org.matrix.android.sdk.flow.FlowSessionKt") in the setup method of the tests.
     */
    fun givenFlowSession(): FlowSession {
        val fakeFlowSession = mockk<FlowSession>()
        every { flow() } returns fakeFlowSession
        return fakeFlowSession
    }

    companion object {

        fun withRoomSummary(roomSummary: RoomSummary) = FakeSession().apply {
            every { getRoomSummary(any()) } returns roomSummary
        }
    }
}
