package org.matrix.android

import android.app.Application
import im.vector.matrix.android.BuildConfig
import org.koin.log.EmptyLogger
import org.koin.standalone.StandAloneContext.startKoin
import timber.log.Timber

class MatrixApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        startKoin(listOf(AppModule(this)), logger = EmptyLogger())
    }

}