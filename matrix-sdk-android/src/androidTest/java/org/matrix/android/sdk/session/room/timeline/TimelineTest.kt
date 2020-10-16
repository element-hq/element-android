/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.session.room.timeline

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.InstrumentedTest

internal class TimelineTest : InstrumentedTest {

    companion object {
        private const val ROOM_ID = "roomId"
    }

    private lateinit var monarchy: Monarchy

//    @Before
//    fun setup() {
//        Timber.plant(Timber.DebugTree())
//        Realm.init(context())
//        val testConfiguration = RealmConfiguration.Builder().name("test-realm")
//                .modules(SessionRealmModule()).build()
//
//        Realm.deleteRealm(testConfiguration)
//        monarchy = Monarchy.Builder().setRealmConfiguration(testConfiguration).build()
//        RoomDataHelper.fakeInitialSync(monarchy, ROOM_ID)
//    }
//
//    private fun createTimeline(initialEventId: String? = null): Timeline {
//        val taskExecutor = TaskExecutor(testCoroutineDispatchers)
//        val tokenChunkEventPersistor = TokenChunkEventPersistor(monarchy)
//        val paginationTask = FakePaginationTask @Inject constructor(tokenChunkEventPersistor)
//        val getContextOfEventTask = FakeGetContextOfEventTask @Inject constructor(tokenChunkEventPersistor)
//        val roomMemberExtractor = SenderRoomMemberExtractor(ROOM_ID)
//        val timelineEventFactory = TimelineEventFactory(roomMemberExtractor, EventRelationExtractor())
//        return DefaultTimeline(
//                ROOM_ID,
//                initialEventId,
//                monarchy.realmConfiguration,
//                taskExecutor,
//                getContextOfEventTask,
//                timelineEventFactory,
//                paginationTask,
//                null)
//    }
//
//    @Test
//    fun backPaginate_shouldLoadMoreEvents_whenPaginateIsCalled() {
//        val timeline = createTimeline()
//        timeline.start()
//        val paginationCount = 30
//        var initialLoad = 0
//        val latch = CountDownLatch(2)
//        var timelineEvents: List<TimelineEvent> = emptyList()
//        timeline.listener = object : Timeline.Listener {
//            override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
//                if (snapshot.isNotEmpty()) {
//                    if (initialLoad == 0) {
//                        initialLoad = snapshot.size
//                    }
//                    timelineEvents = snapshot
//                    latch.countDown()
//                    timeline.paginate(Timeline.Direction.BACKWARDS, paginationCount)
//                }
//            }
//        }
//        latch.await()
//        timelineEvents.size shouldBeEqualTo initialLoad + paginationCount
//        timeline.dispose()
//    }
}
