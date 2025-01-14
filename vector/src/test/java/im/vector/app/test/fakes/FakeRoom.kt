/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import io.mockk.mockk
import org.matrix.android.sdk.api.session.room.Room

class FakeRoom(
        private val fakeLocationSharingService: FakeLocationSharingService = FakeLocationSharingService(),
        private val fakeSendService: FakeSendService = FakeSendService(),
        private val fakeTimelineService: FakeTimelineService = FakeTimelineService(),
        private val fakeRelationService: FakeRelationService = FakeRelationService(),
        private val fakeStateService: FakeStateService = FakeStateService(),
        private val fakePollHistoryService: FakePollHistoryService = FakePollHistoryService(),
) : Room by mockk() {

    override fun locationSharingService() = fakeLocationSharingService

    override fun sendService() = fakeSendService

    override fun timelineService() = fakeTimelineService

    override fun relationService() = fakeRelationService

    override fun stateService() = fakeStateService

    override fun pollHistoryService() = fakePollHistoryService
}
