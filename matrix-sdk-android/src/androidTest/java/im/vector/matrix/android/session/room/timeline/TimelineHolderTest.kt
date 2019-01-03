package im.vector.matrix.android.session.room.timeline

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.support.test.annotation.UiThreadTest
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.InstrumentedTest
import im.vector.matrix.android.LiveDataTestObserver
import im.vector.matrix.android.api.thread.MainThreadExecutor
import im.vector.matrix.android.internal.session.room.members.RoomMemberExtractor
import im.vector.matrix.android.internal.session.room.timeline.DefaultTimelineHolder
import im.vector.matrix.android.internal.session.room.timeline.TimelineBoundaryCallback
import im.vector.matrix.android.internal.session.room.timeline.TokenChunkEventPersistor
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.PagingRequestHelper
import im.vector.matrix.android.testCoroutineDispatchers
import io.realm.Realm
import io.realm.RealmConfiguration
import org.amshove.kluent.shouldEqual
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
        val timelineHolder = DefaultTimelineHolder(roomId, monarchy, taskExecutor, boundaryCallback, getContextOfEventTask, RoomMemberExtractor(monarchy, roomId))
        val timelineObserver = LiveDataTestObserver.test(timelineHolder.timeline())
        timelineObserver.awaitNextValue().assertHasValue()
        var pagedList = timelineObserver.value()
        pagedList.size shouldEqual 30
        (0 until pagedList.size).map {
            pagedList.loadAround(it)
        }
        timelineObserver.awaitNextValue().assertHasValue()
        pagedList = timelineObserver.value()
        pagedList.size shouldEqual 60
    }


}