package im.vector.riotredesign.core.di

import android.content.Context
import im.vector.riotredesign.core.resources.LocaleProvider
import org.koin.dsl.module.module

class AppModule(private val context: Context) {

    val definition = module {

        single {
            LocaleProvider(context.resources)
        }

    }
}