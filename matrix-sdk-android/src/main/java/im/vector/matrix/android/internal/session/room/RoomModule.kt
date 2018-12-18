package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.room.SendService
import im.vector.matrix.android.api.session.room.TimelineHolder
import im.vector.matrix.android.api.session.room.send.EventFactory
import im.vector.matrix.android.internal.session.DefaultSession
import im.vector.matrix.android.internal.session.room.members.DefaultLoadRoomMembersTask
import im.vector.matrix.android.internal.session.room.members.LoadRoomMembersTask
import im.vector.matrix.android.internal.session.room.members.RoomMemberExtractor
import im.vector.matrix.android.internal.session.room.send.DefaultSendService
import im.vector.matrix.android.internal.session.room.timeline.DefaultGetContextOfEventTask
import im.vector.matrix.android.internal.session.room.timeline.DefaultPaginationTask
import im.vector.matrix.android.internal.session.room.timeline.DefaultTimelineHolder
import im.vector.matrix.android.internal.session.room.timeline.GetContextOfEventTask
import im.vector.matrix.android.internal.session.room.timeline.PaginationTask
import im.vector.matrix.android.internal.session.room.timeline.TimelineBoundaryCallback
import im.vector.matrix.android.internal.session.room.timeline.TokenChunkEventPersistor
import im.vector.matrix.android.internal.util.PagingRequestHelper
import org.koin.dsl.module.module
import retrofit2.Retrofit
import java.util.concurrent.Executors


class RoomModule {

    val definition = module(override = true) {

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(RoomAPI::class.java)
        }

        scope(DefaultSession.SCOPE) {
            DefaultLoadRoomMembersTask(get(), get()) as LoadRoomMembersTask
        }

        scope(DefaultSession.SCOPE) {
            TokenChunkEventPersistor(get())
        }

        scope(DefaultSession.SCOPE) {
            DefaultPaginationTask(get(), get()) as PaginationTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultGetContextOfEventTask(get(), get()) as GetContextOfEventTask
        }

        scope(DefaultSession.SCOPE) {
            val sessionParams = get<SessionParams>()
            EventFactory(sessionParams.credentials)
        }

        factory { (roomId: String) ->
            val helper = PagingRequestHelper(Executors.newSingleThreadExecutor())
            val timelineBoundaryCallback = TimelineBoundaryCallback(roomId, get(), get(), get(), helper)
            val roomMemberExtractor = RoomMemberExtractor(get(), roomId)
            DefaultTimelineHolder(roomId, get(), get(), timelineBoundaryCallback, get(), roomMemberExtractor) as TimelineHolder
        }

        factory { (roomId: String) ->
            DefaultSendService(roomId, get(), get()) as SendService
        }

    }
}
