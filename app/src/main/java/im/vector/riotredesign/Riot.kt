package im.vector.riotredesign

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import im.vector.matrix.android.BuildConfig
import im.vector.riotredesign.core.di.AppModule
import org.koin.log.EmptyLogger
import org.koin.standalone.StandAloneContext.startKoin
import timber.log.Timber

class Riot : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        AndroidThreeTen.init(this)
        startKoin(listOf(AppModule(this)), logger = EmptyLogger())
    }

}