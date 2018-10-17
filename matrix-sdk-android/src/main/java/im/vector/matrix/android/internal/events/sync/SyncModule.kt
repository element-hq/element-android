package im.vector.matrix.android.internal.events.sync

import im.vector.matrix.android.internal.events.sync.job.SyncThread
import im.vector.matrix.android.internal.session.DefaultSession
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import retrofit2.Retrofit


class SyncModule : Module {

    override fun invoke(): ModuleDefinition = module(override = true) {

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(SyncAPI::class.java)
        }

        scope(DefaultSession.SCOPE) {
            RoomSyncHandler(get())
        }

        scope(DefaultSession.SCOPE) {
            SyncResponseHandler(get(), get())
        }

        scope(DefaultSession.SCOPE) {
            SyncRequest(get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            SyncTokenStore(get())
        }

        scope(DefaultSession.SCOPE) {
            SyncThread(get(), get(), get())
        }


    }.invoke()
}
