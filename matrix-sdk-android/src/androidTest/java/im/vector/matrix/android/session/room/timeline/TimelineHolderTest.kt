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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.annotation.UiThreadTest
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.InstrumentedTest
import im.vector.matrix.android.LiveDataTestObserver
import im.vector.matrix.android.MainThreadExecutor
import im.vector.matrix.android.internal.session.room.members.RoomMemberExtractor
import im.vector.matrix.android.internal.session.room.timeline.DefaultTimelineService
import im.vector.matrix.android.internal.session.room.timeline.TimelineBoundaryCallback
import im.vector.matrix.android.internal.session.room.timeline.TokenChunkEventPersistor
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.PagingRequestHelper
import im.vector.matrix.android.testCoroutineDispatchers
import io.realm.Realm
import io.realm.RealmConfiguration
import org.junit.Before
import org.junit.Rule
import org.junit.Test

internal class TimelineHolderTest : InstrumentedTest {

    @get:Rule val testRule = InstantTaskExecutorRule()
    private lateinit var monarchy: Monarchy

    @Before
    fun setup() {
        Realm.init(context())
        val testConfiguration = RealmConfiguration.Builder().name("test-realm").build()
        Realm.deleteRealm(testConfiguration)
        monarchy = Monarchy.Builder().setRealmConfiguration(testConfiguration).build()
    }

    @Test
    @UiThreadTest
    fun backPaginate_shouldLoadMoreEvents_whenLoadAroundIsCalled() {
        val roomId = "roomId"
        val taskExecutor = TaskExecutor(testCoroutineDispatchers)
        val tokenChunkEventPersistor = TokenChunkEventPersistor(monarchy)
        val paginationTask = FakePaginationTask(tokenChunkEventPersistor)
        val getContextOfEventTask = FakeGetContextOfEventTask(tokenChunkEventPersistor)
        val boundaryCallback = TimelineBoundaryCallback(roomId, taskExecutor, paginationTask, monarchy, PagingRequestHelper(MainThreadExecutor()))

        RoomDataHelper.fakeInitialSync(monarchy, roomId)
        val timelineHolder = DefaultTimelineService(roomId, monarchy, taskExecutor, boundaryCallback, getContextOfEventTask, RoomMemberExtractor(monarchy, roomId))
        val timelineObserver = LiveDataTestObserver.test(timelineHolder.timeline())
        timelineObserver.awaitNextValue().assertHasValue()
        var timelineData = timelineObserver.value()
        timelineData.events.size shouldEqual 30
        (0 until timelineData.events.size).map {
            timelineData.events.loadAround(it)
        }
        timelineObserver.awaitNextValue().assertHasValue()
        timelineData = timelineObserver.value()
        timelineData.events.size shouldEqual 60
    }


}