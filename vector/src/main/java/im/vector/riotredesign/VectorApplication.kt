/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.multidex.MultiDex
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyController
import com.facebook.stetho.Stetho
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.jakewharton.threetenabp.AndroidThreeTen
import im.vector.matrix.android.api.Matrix
import im.vector.riotredesign.core.di.AppModule
import im.vector.riotredesign.features.configuration.VectorConfiguration
import im.vector.riotredesign.features.home.HomeModule
import im.vector.riotredesign.features.lifecycle.VectorActivityLifecycleCallbacks
import im.vector.riotredesign.features.rageshake.VectorFileLogger
import im.vector.riotredesign.features.rageshake.VectorUncaughtExceptionHandler
import im.vector.riotredesign.features.roomdirectory.RoomDirectoryModule
import org.koin.android.ext.android.inject
import org.koin.log.EmptyLogger
import org.koin.standalone.StandAloneContext.startKoin
import timber.log.Timber


class VectorApplication : Application() {

    lateinit var appContext: Context
    val vectorConfiguration: VectorConfiguration by inject()

    override fun onCreate() {
        super.onCreate()
        appContext = this

        VectorUncaughtExceptionHandler.activate(this)

        // Log
        VectorFileLogger.init(this)
        Timber.plant(Timber.DebugTree(), VectorFileLogger)

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }

        AndroidThreeTen.init(this)
        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))
        EpoxyController.defaultDiffingHandler = EpoxyAsyncUtil.getAsyncBackgroundHandler()
        EpoxyController.defaultModelBuildingHandler = EpoxyAsyncUtil.getAsyncBackgroundHandler()
        val appModule = AppModule(applicationContext).definition
        val homeModule = HomeModule().definition
        val roomDirectoryModule = RoomDirectoryModule().definition
        startKoin(listOf(appModule, homeModule, roomDirectoryModule), logger = EmptyLogger())
        Matrix.getInstance().setApplicationFlavor(BuildConfig.FLAVOR_DESCRIPTION)
        registerActivityLifecycleCallbacks(VectorActivityLifecycleCallbacks())
        vectorConfiguration.initConfiguration()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        vectorConfiguration.onConfigurationChanged(newConfig)
    }

}