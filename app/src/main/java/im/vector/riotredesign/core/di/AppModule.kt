package im.vector.riotredesign.core.di

import android.content.Context
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixOptions
import im.vector.riotredesign.core.resources.LocaleProvider
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

class AppModule(private val context: Context) : Module {

    override fun invoke(): ModuleDefinition = module {

        single(createOnStart = true) {
            val matrixOptions = MatrixOptions(context)
            Matrix(matrixOptions)
        }

        single {
            LocaleProvider(context.resources)
        }

    }.invoke()
}