/*
 * Copyright (c) 2021 New Vector Ltd
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
