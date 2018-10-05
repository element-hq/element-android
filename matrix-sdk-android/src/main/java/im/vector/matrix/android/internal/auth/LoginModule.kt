package im.vector.matrix.android.internal.auth

import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.internal.DefaultSession
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
