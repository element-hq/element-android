package im.vector.matrix.android.internal.di

import im.vector.matrix.android.api.MatrixOptions
import im.vector.matrix.android.api.login.CredentialsStore
import im.vector.matrix.android.api.login.data.Credentials
import im.vector.matrix.android.api.login.data.MyObjectBox
import im.vector.matrix.android.internal.MatrixCoroutineDispatchers
import im.vector.matrix.android.internal.login.db.ObjectBoxCredentialsStore
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.asCoroutineDispatcher
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module


class MatrixModule(private val options: MatrixOptions) : Module {

    override fun invoke(): ModuleDefinition = module {

        single {
            MatrixCoroutineDispatchers(io = Dispatchers.IO, computation = Dispatchers.IO, main = options.mainExecutor.asCoroutineDispatcher())
        }

        single {
            MyObjectBox.builder().androidContext(options.context).build()
        }

        single {
            val boxStore = get() as BoxStore
            boxStore.boxFor(Credentials::class.java) as Box<Credentials>
        }

        single {
            ObjectBoxCredentialsStore(get()) as CredentialsStore
        }

    }.invoke()
}