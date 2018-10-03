package im.vector.matrix.core.internal

import im.vector.matrix.core.api.MatrixOptions
import im.vector.matrix.core.api.login.CredentialsStore
import im.vector.matrix.core.internal.login.db.InMemoryCredentialsStore
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
            InMemoryCredentialsStore() as CredentialsStore
        }

    }.invoke()
}