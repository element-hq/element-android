package im.vector.matrix.core.internal.login

import im.vector.matrix.core.api.login.Authenticator
import im.vector.matrix.core.api.login.data.HomeServerConnectionConfig
import im.vector.matrix.core.internal.DefaultSession
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module
import retrofit2.Retrofit

class LoginModule(private val connectionConfig: HomeServerConnectionConfig) : Module {

    override fun invoke(): ModuleDefinition = module {
        scope(DefaultSession.SCOPE) {
            Retrofit.Builder()
                    .client(get())
                    .baseUrl(connectionConfig.hsUri)
                    .addConverterFactory(get())
                    .addCallAdapterFactory(get())
                    .build()
        }

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(LoginApi::class.java)
        }

        scope(DefaultSession.SCOPE) {
            DefaultAuthenticator(get(), get(), get(), get()) as Authenticator
        }

    }.invoke()
}
