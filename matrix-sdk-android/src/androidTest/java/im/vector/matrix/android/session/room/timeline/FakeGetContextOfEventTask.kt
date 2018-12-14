package im.vector.matrix.android.session.room.timeline

import arrow.core.Try
import im.vector.matrix.android.internal.session.room.timeline.GetContextOfEventTask
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.session.room.timeline.TokenChunkEvent
import im.vector.matrix.android.internal.session.room.timeline.TokenChunkEventPersistor
import kotlin.random.Random

internal class FakeGetContextOfEventTask(private val tokenChunkEventPersistor: TokenChunkEventPersistor) : GetContextOfEventTask {

    override fun execute(params: GetContextOfEventTask.Params): Try<TokenChunkEvent> {
        val fakeEvents = RoomDataHelper.createFakeListOfEvents(30)
        val tokenChunkEvent = FakeTokenChunkEvent(
                Random.nextLong(System.currentTimeMillis()).toString(),
                Random.nextLong(System.currentTimeMillis()).toString(),
                fakeEvents
        )
        return tokenChunkEventPersistor.insertInDb(tokenChunkEvent, params.roomId, PaginationDirection.BACKWARDS)
                .map { tokenChunkEvent }
    }


}