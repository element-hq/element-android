package im.vector.riotredesign.core.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import im.vector.riotredesign.core.resources.LocaleProvider
import im.vector.riotredesign.core.resources.StringProvider
import im.vector.riotredesign.features.home.room.list.RoomSelectionRepository
import org.koin.dsl.module.module

class AppModule(private val context: Context) {

    val definition = module {

        single {
            LocaleProvider(context.resources)
        }

        single {
            StringProvider(context.resources)
        }

        single {
            context.getSharedPreferences("im.vector.riot", MODE_PRIVATE)
        }

        single {
            RoomSelectionRepository(get())
        }

    }
}