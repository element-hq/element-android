package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.room.SendService
import im.vector.matrix.android.api.session.room.TimelineHolder
import im.vector.matrix.android.api.session.room.send.EventFactory
import im.vector.matrix.android.internal.session.DefaultSession
import im.vector.matrix.android.internal.session.room.members.LoadRoomMembersRequest
import im.vector.matrix.android.internal.session.room.send.DefaultSendService
import im.vector.matrix.android.internal.session.room.timeline.DefaultTimelineHolder
import im.vector.matrix.android.internal.session.room.timeline.GetContextOfEventRequest
import im.vector.matrix.android.internal.session.room.timeline.PaginationRequest
import im.vector.matrix.android.internal.session.room.timeline.TimelineBoundaryCallback
import im.vector.matrix.android.internal.util.PagingRequestHelper
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import retrofit2.Retrofit
import java.util.concurrent.Executors


class RoomModule : Module {

    override fun invoke(): ModuleDefinition = module(override = true) {

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(RoomAPI::class.java)
        }

        scope(DefaultSession.SCOPE) {
            LoadRoomMembersRequest(get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            PaginationRequest(get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            GetContextOfEventRequest(get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            val sessionParams = get<SessionParams>()
            EventFactory(sessionParams.credentials)
        }

        factory { (roomId: String) ->
            val helper = PagingRequestHelper(Executors.newSingleThreadExecutor())
            val timelineBoundaryCallback = TimelineBoundaryCallback(roomId, get(), get(), helper)
            DefaultTimelineHolder(roomId, get(), timelineBoundaryCallback, get()) as TimelineHolder
        }

        factory { (roomId: String) ->
            DefaultSendService(roomId, get(), get()) as SendService
        }

    }.invoke()
}
