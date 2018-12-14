package im.vector.matrix.android.internal.session.group

import im.vector.matrix.android.internal.session.DefaultSession
import org.koin.dsl.module.module
import retrofit2.Retrofit

class GroupModule {

    val definition = module(override = true) {

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(GroupAPI::class.java)
        }

        scope(DefaultSession.SCOPE) {
            DefaultGetGroupDataTask(get(), get()) as GetGroupDataTask
        }

    }
}