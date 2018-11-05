package im.vector.matrix.android.internal.session.group

import im.vector.matrix.android.internal.session.DefaultSession
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import retrofit2.Retrofit

class GroupModule : Module {

    override fun invoke(): ModuleDefinition = module(override = true) {

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(GroupAPI::class.java)
        }

        scope(DefaultSession.SCOPE) {
            GetGroupSummaryRequest(get(), get(), get())
        }

    }.invoke()
}