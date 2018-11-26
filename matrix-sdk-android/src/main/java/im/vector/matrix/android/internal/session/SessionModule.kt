package im.vector.matrix.android.internal.session

import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.internal.database.DatabaseInstances
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.session.events.prune.EventsPruner
import im.vector.matrix.android.internal.session.group.DefaultGroupService
import im.vector.matrix.android.internal.session.group.GroupSummaryUpdater
import im.vector.matrix.android.internal.session.room.DefaultRoomService
import im.vector.matrix.android.internal.session.room.RoomAvatarResolver
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.room.members.RoomDisplayNameResolver
import im.vector.matrix.android.internal.session.room.members.RoomMemberDisplayNameResolver
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import retrofit2.Retrofit

internal class SessionModule(private val sessionParams: SessionParams) : Module {

    override fun invoke(): ModuleDefinition = module(override = true) {

        scope(DefaultSession.SCOPE) {
            sessionParams
        }

        scope(DefaultSession.SCOPE) {
            val retrofitBuilder = get<Retrofit.Builder>()
            retrofitBuilder
                    .baseUrl(sessionParams.homeServerConnectionConfig.homeServerUri.toString())
                    .build()
        }

        scope(DefaultSession.SCOPE) {
            RoomMemberDisplayNameResolver()
        }

        scope(DefaultSession.SCOPE) {
            RoomDisplayNameResolver(get<DatabaseInstances>().disk, get(), sessionParams.credentials)
        }

        scope(DefaultSession.SCOPE) {
            RoomAvatarResolver(get<DatabaseInstances>().disk, sessionParams.credentials)
        }

        scope(DefaultSession.SCOPE) {
            DefaultRoomService(get<DatabaseInstances>().disk) as RoomService
        }


        scope(DefaultSession.SCOPE) {
            DefaultGroupService(get<DatabaseInstances>().disk) as GroupService
        }

        scope(DefaultSession.SCOPE) {
            val disk = get<DatabaseInstances>().disk
            val roomSummaryUpdater = RoomSummaryUpdater(disk, get(), get(), get(), sessionParams.credentials)
            val groupSummaryUpdater = GroupSummaryUpdater(disk)
            val eventsPruner = EventsPruner(disk)
            listOf<LiveEntityObserver>(roomSummaryUpdater, groupSummaryUpdater, eventsPruner)
        }


    }.invoke()


}
