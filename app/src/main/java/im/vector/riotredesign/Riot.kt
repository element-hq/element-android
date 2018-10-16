package im.vector.riotredesign

import android.app.Application
import im.vector.matrix.android.BuildConfig
import im.vector.riotredesign.core.di.AppModule
import org.koin.standalone.StandAloneContext.startKoin
import timber.log.Timber

class Riot : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        startKoin(listOf(AppModule(this)))
    }

}