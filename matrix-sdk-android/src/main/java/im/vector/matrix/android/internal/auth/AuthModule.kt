package im.vector.matrix.android.internal.auth

import android.content.Context
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.internal.auth.db.ObjectBoxSessionParams
import im.vector.matrix.android.internal.auth.db.ObjectBoxSessionParamsMapper
import im.vector.matrix.android.internal.auth.db.ObjectBoxSessionParamsStore
import io.objectbox.Box
import io.objectbox.BoxStore
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

private const val AUTH_BOX_STORE = "AUTH_BOX_STORE"

class AuthModule(private val context: Context) : Module {

    override fun invoke(): ModuleDefinition = module {

        single {
            DefaultAuthenticator(get(), get(), get(), get()) as Authenticator
        }

        single(name = AUTH_BOX_STORE) {
            MyObjectBox.builder().androidContext(context).build()
        }


        single {
            val boxStore = get(name = AUTH_BOX_STORE) as BoxStore
            boxStore.boxFor(ObjectBoxSessionParams::class.java) as Box<ObjectBoxSessionParams>
        }

        single {
            ObjectBoxSessionParamsStore(ObjectBoxSessionParamsMapper((get())), get()) as SessionParamsStore
        }

    }.invoke()
}
