package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.api.session.room.TimelineHolder
import im.vector.matrix.android.internal.session.DefaultSession
import im.vector.matrix.android.internal.session.room.members.LoadRoomMembersRequest
import im.vector.matrix.android.internal.session.room.timeline.DefaultTimelineHolder
import im.vector.matrix.android.internal.session.room.timeline.PaginationRequest
import im.vector.matrix.android.internal.session.room.timeline.TimelineBoundaryCallback
import org.koin.core.parameter.parametersOf
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
            LoadRoomMembersRequest(get(), get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            PaginationRequest(get(), get(), get(), get())
        }

        factory {
            val roomId: String = it[0]
            TimelineBoundaryCallback(roomId, get(), get(), Executors.newSingleThreadExecutor())
        }

        factory {
            val roomId: String = it[0]
            DefaultTimelineHolder(roomId, get(), get(parameters = { parametersOf(roomId) })) as TimelineHolder
        }

    }.invoke()
}
