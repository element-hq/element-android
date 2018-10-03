package im.vector.riotredesign

import android.app.Application
import org.koin.standalone.StandAloneContext.startKoin

class Riot : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin(emptyList())
    }

}