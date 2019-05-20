/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.session.room.timeline

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.InstrumentedTest
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.session.room.EventRelationExtractor
import im.vector.matrix.android.internal.session.room.EventRelationsAggregationUpdater
import im.vector.matrix.android.internal.session.room.members.SenderRoomMemberExtractor
import im.vector.matrix.android.internal.session.room.timeline.DefaultTimeline
import im.vector.matrix.android.internal.session.room.timeline.TimelineEventFactory
import im.vector.matrix.android.internal.session.room.timeline.TokenChunkEventPersistor
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.testCoroutineDispatchers
import io.realm.Realm
import io.realm.RealmConfiguration
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.util.concurrent.CountDownLatch

internal class TimelineTest : InstrumentedTest {

    companion object {
        private const val ROOM_ID = "roomId"
    }

    private lateinit var monarchy: Monarchy

    @Before
    fun setup() {
        Timber.plant(Timber.DebugTree())
        Realm.init(context())
        val testConfiguration = RealmConfiguration.Builder().name("test-realm").build()
        Realm.deleteRealm(testConfiguration)
        monarchy = Monarchy.Builder().setRealmConfiguration(testConfiguration).build()
        RoomDataHelper.fakeInitialSync(monarchy, ROOM_ID)
    }

    private fun createTimeline(initialEventId: String? = null): Timeline {
        val taskExecutor = TaskExecutor(testCoroutineDispatchers)
        val erau = EventRelationsAggregationUpdater(Credentials("", "", "", null, null))
        val tokenChunkEventPersistor = TokenChunkEventPersistor(monarchy, erau)
        val paginationTask = FakePaginationTask(tokenChunkEventPersistor)
        val getContextOfEventTask = FakeGetContextOfEventTask(tokenChunkEventPersistor)
        val roomMemberExtractor = SenderRoomMemberExtractor(ROOM_ID)
        val timelineEventFactory = TimelineEventFactory(roomMemberExtractor, EventRelationExtractor())
        return DefaultTimeline(
                ROOM_ID,
                initialEventId,
                monarchy.realmConfiguration,
                taskExecutor,
                getContextOfEventTask,
                timelineEventFactory,
                paginationTask,
                null)
    }

    @Test
    fun backPaginate_shouldLoadMoreEvents_whenPaginateIsCalled() {
        val timeline = createTimeline()
        timeline.start()
        val paginationCount = 30
        var initialLoad = 0
        val latch = CountDownLatch(2)
        var timelineEvents: List<TimelineEvent> = emptyList()
        timeline.listener = object : Timeline.Listener {
            override fun onUpdated(snapshot: List<TimelineEvent>) {
                if (snapshot.isNotEmpty()) {
                    if (initialLoad == 0) {
                        initialLoad = snapshot.size
                    }
                    timelineEvents = snapshot
                    latch.countDown()
                    timeline.paginate(Timeline.Direction.BACKWARDS, paginationCount)
                }
            }
        }
        latch.await()
        timelineEvents.size shouldEqual initialLoad + paginationCount
        timeline.dispose()
    }


}