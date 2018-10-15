package im.vector.matrix.android.internal.auth

import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.internal.auth.db.RealmSessionParamsStore
import im.vector.matrix.android.internal.auth.db.SessionParamsMapper
import io.realm.RealmConfiguration
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

class AuthModule : Module {

    override fun invoke(): ModuleDefinition = module {

        single {
            DefaultAuthenticator(get(), get(), get()) as Authenticator
        }

        single {
            val mapper = SessionParamsMapper((get()))
            val realmConfiguration = RealmConfiguration.Builder().name("matrix-sdk-auth").deleteRealmIfMigrationNeeded().build()
            RealmSessionParamsStore(mapper, realmConfiguration) as SessionParamsStore
        }

    }.invoke()
}
