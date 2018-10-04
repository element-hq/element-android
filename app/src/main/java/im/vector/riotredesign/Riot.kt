package im.vector.riotredesign

import android.app.Application
import im.vector.riotredesign.core.di.AppModule
import org.koin.standalone.StandAloneContext.startKoin

class Riot : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin(listOf(AppModule(this)))
    }

}