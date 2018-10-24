package im.vector.matrix.android.internal.session.room

import im.vector.matrix.android.internal.session.DefaultSession
import im.vector.matrix.android.internal.session.room.timeline.PaginationRequest
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import retrofit2.Retrofit


class RoomModule : Module {

    override fun invoke(): ModuleDefinition = module(override = true) {

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(RoomAPI::class.java)
        }

        scope(DefaultSession.SCOPE) {
            PaginationRequest(get(), get(), get(), get())
        }
    }.invoke()
}
