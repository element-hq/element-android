package im.vector.matrix.android.internal.di

import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.internal.session.DefaultSession
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import retrofit2.Retrofit

class SessionModule(private val connectionConfig: HomeServerConnectionConfig) : Module {

    override fun invoke(): ModuleDefinition = module {
        scope(DefaultSession.SCOPE) {
            val retrofitBuilder = get() as Retrofit.Builder
            retrofitBuilder
                    .baseUrl(connectionConfig.hsUri)
                    .build()
        }
    }.invoke()
}
