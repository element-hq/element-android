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
import android.os.Handler
import android.os.HandlerThread
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.multidex.MultiDex
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyController
import com.facebook.stetho.Stetho
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.jakewharton.threetenabp.AndroidThreeTen
import im.vector.matrix.android.api.Matrix
import im.vector.riotredesign.core.di.HasInjector
import im.vector.riotredesign.core.di.VectorComponent
import im.vector.riotredesign.features.configuration.VectorConfiguration
import im.vector.riotredesign.features.lifecycle.VectorActivityLifecycleCallbacks
import im.vector.riotredesign.features.rageshake.VectorFileLogger
import im.vector.riotredesign.features.rageshake.VectorUncaughtExceptionHandler
import timber.log.Timber
import javax.inject.Inject


class VectorApplication : Application(), HasInjector<VectorComponent> {

    lateinit var appContext: Context
    //font thread handler
    @Inject lateinit var vectorConfiguration: VectorConfiguration
    @Inject lateinit var emojiCompatFontProvider: EmojiCompatFontProvider
    lateinit var vectorComponent: VectorComponent
    private var fontThreadHandler: Handler? = null

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

        Matrix.getInstance().setApplicationFlavor(BuildConfig.FLAVOR_DESCRIPTION)
        registerActivityLifecycleCallbacks(VectorActivityLifecycleCallbacks())
        val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs
        )
        FontsContractCompat.requestFont(this, fontRequest, emojiCompatFontProvider, getFontThreadHandler())
        vectorConfiguration.initConfiguration()
    }

    override fun injector(): VectorComponent {
        return vectorComponent
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        vectorConfiguration.onConfigurationChanged(newConfig)
    }

    private fun getFontThreadHandler(): Handler {
        return fontThreadHandler ?: createFontThreadHandler().also {
            fontThreadHandler = it
        }
    }

    private fun createFontThreadHandler(): Handler {
        val handlerThread = HandlerThread("fonts")
        handlerThread.start()
        return Handler(handlerThread.looper)
    }

}