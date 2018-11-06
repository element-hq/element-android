package im.vector.matrix.android.internal.di

import im.vector.matrix.android.api.MatrixOptions
import im.vector.matrix.android.api.thread.MainThreadExecutor
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module


class MatrixModule(private val options: MatrixOptions) : Module {

    override fun invoke(): ModuleDefinition = module {

        single {
            options.context
        }

        single {
            MatrixCoroutineDispatchers(io = Dispatchers.IO, computation = Dispatchers.IO, main = MainThreadExecutor().asCoroutineDispatcher())
        }

        single {
            BackgroundDetectionObserver()
        }

    }.invoke()
}