package im.vector.matrix.android.internal.auth

import android.content.Context
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.auth.CredentialsStore
import im.vector.matrix.android.internal.auth.data.Credentials
import im.vector.matrix.android.internal.auth.data.MyObjectBox
import im.vector.matrix.android.internal.auth.db.ObjectBoxCredentialsStore
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
            boxStore.boxFor(Credentials::class.java) as Box<Credentials>
        }

        single {
            ObjectBoxCredentialsStore(get()) as CredentialsStore
        }

    }.invoke()
}
