package im.vector.matrix.android.internal.session

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.internal.auth.data.SessionParams
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.session.events.prune.EventsPruner
import im.vector.matrix.android.internal.session.group.DefaultGroupService
import im.vector.matrix.android.internal.session.group.GroupSummaryUpdater
import im.vector.matrix.android.internal.session.room.DefaultRoomService
import im.vector.matrix.android.internal.session.room.RoomAvatarResolver
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.room.members.RoomDisplayNameResolver
import im.vector.matrix.android.internal.session.room.members.RoomMemberDisplayNameResolver
import io.realm.RealmConfiguration
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import retrofit2.Retrofit

class SessionModule(private val sessionParams: SessionParams) : Module {

    override fun invoke(): ModuleDefinition = module(override = true) {

        scope(DefaultSession.SCOPE) {
            RealmConfiguration.Builder()
                    .name(sessionParams.credentials.userId)
                    .deleteRealmIfMigrationNeeded()
                    .build()
        }

        scope(DefaultSession.SCOPE) {
            Monarchy.Builder()
                    .setRealmConfiguration(get())
                    .build()
        }

        scope(DefaultSession.SCOPE) {
            val retrofitBuilder = get() as Retrofit.Builder
            retrofitBuilder
                    .baseUrl(sessionParams.homeServerConnectionConfig.homeServerUri.toString())
                    .build()
        }

        scope(DefaultSession.SCOPE) {
            RoomMemberDisplayNameResolver()
        }

        scope(DefaultSession.SCOPE) {
            RoomDisplayNameResolver(get(), get(), sessionParams.credentials)
        }

        scope(DefaultSession.SCOPE) {
            RoomAvatarResolver(get(), sessionParams.credentials)
        }

        scope(DefaultSession.SCOPE) {
            DefaultRoomService(get()) as RoomService
        }


        scope(DefaultSession.SCOPE) {
            DefaultGroupService(get()) as GroupService
        }

        scope(DefaultSession.SCOPE) {
            val roomSummaryUpdater = RoomSummaryUpdater(get(), get(), get(), get(), sessionParams.credentials)
            val groupSummaryUpdater = GroupSummaryUpdater(get(), get())
            val eventsPruner = EventsPruner(get())
            listOf<LiveEntityObserver>(roomSummaryUpdater, groupSummaryUpdater, eventsPruner)
        }


    }.invoke()


}
