package im.vector.matrix.android.session.room.timeline

import arrow.core.Try
import im.vector.matrix.android.internal.session.room.timeline.PaginationTask
import im.vector.matrix.android.internal.session.room.timeline.TokenChunkEvent
import im.vector.matrix.android.internal.session.room.timeline.TokenChunkEventPersistor
import kotlin.random.Random

internal class FakePaginationTask(private val tokenChunkEventPersistor: TokenChunkEventPersistor) : PaginationTask {

    override fun execute(params: PaginationTask.Params): Try<TokenChunkEvent> {
        val fakeEvents = RoomDataHelper.createFakeListOfEvents(30)
        val tokenChunkEvent = FakeTokenChunkEvent(params.from, Random.nextLong(System.currentTimeMillis()).toString(), fakeEvents)
        return tokenChunkEventPersistor.insertInDb(tokenChunkEvent, params.roomId, params.direction)
                .map { tokenChunkEvent }
    }

}

