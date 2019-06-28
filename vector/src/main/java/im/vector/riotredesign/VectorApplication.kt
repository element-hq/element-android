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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyController
import com.facebook.stetho.Stetho
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.jakewharton.threetenabp.AndroidThreeTen
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixConfiguration
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.riotredesign.core.di.ActiveSessionHolder
import im.vector.riotredesign.core.di.DaggerVectorComponent
import im.vector.riotredesign.core.di.HasVectorInjector
import im.vector.riotredesign.core.di.VectorComponent
import im.vector.riotredesign.core.extensions.configureAndStart
import im.vector.riotredesign.features.configuration.VectorConfiguration
import im.vector.riotredesign.features.lifecycle.VectorActivityLifecycleCallbacks
import im.vector.riotredesign.features.notifications.NotificationDrawerManager
import im.vector.riotredesign.features.notifications.NotificationUtils
import im.vector.riotredesign.features.notifications.PushRuleTriggerListener
import im.vector.riotredesign.features.rageshake.VectorFileLogger
import im.vector.riotredesign.features.rageshake.VectorUncaughtExceptionHandler
import im.vector.riotredesign.features.version.getVersion
import im.vector.riotredesign.push.fcm.FcmHelper
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import im.vector.riotredesign.core.utils.initKnownEmojiHashSet
import javax.inject.Inject

class VectorApplication : Application(), HasVectorInjector, MatrixConfiguration.Provider, androidx.work.Configuration.Provider {


    lateinit var appContext: Context
    //font thread handler
    @Inject lateinit var authenticator: Authenticator
    @Inject lateinit var vectorConfiguration: VectorConfiguration
    @Inject lateinit var emojiCompatFontProvider: EmojiCompatFontProvider
    @Inject lateinit var vectorUncaughtExceptionHandler: VectorUncaughtExceptionHandler
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var pushRuleTriggerListener: PushRuleTriggerListener
    lateinit var vectorComponent: VectorComponent
    private var fontThreadHandler: Handler? = null


//    var slowMode = false


    override fun onCreate() {
        super.onCreate()
        appContext = this
        vectorComponent = DaggerVectorComponent.factory().create(this)
        vectorComponent.inject(this)
        vectorUncaughtExceptionHandler.activate(this)
        // Log
        VectorFileLogger.init(this)
        Timber.plant(Timber.DebugTree(), VectorFileLogger)
        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }
        logInfo()
        AndroidThreeTen.init(this)
        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))
        EpoxyController.defaultDiffingHandler = EpoxyAsyncUtil.getAsyncBackgroundHandler()
        EpoxyController.defaultModelBuildingHandler = EpoxyAsyncUtil.getAsyncBackgroundHandler()
        registerActivityLifecycleCallbacks(VectorActivityLifecycleCallbacks())
        val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs
        )
        FontsContractCompat.requestFont(this, fontRequest, emojiCompatFontProvider, getFontThreadHandler())
        vectorConfiguration.initConfiguration()
        NotificationUtils.createNotificationChannels(applicationContext)
        if (authenticator.hasAuthenticatedSessions() && !activeSessionHolder.hasActiveSession()) {
            val lastAuthenticatedSession = authenticator.getLastAuthenticatedSession()!!
            activeSessionHolder.setActiveSession(lastAuthenticatedSession)
            lastAuthenticatedSession.configureAndStart(pushRuleTriggerListener)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun entersForeground() {
                FcmHelper.onEnterForeground(appContext)
                activeSessionHolder.getSafeActiveSession()?.also {
                    it.stopAnyBackgroundSync()
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun entersBackground() {
                Timber.i("App entered background") // call persistInfo
                notificationDrawerManager.persistInfo()
                FcmHelper.onEnterBackground(appContext, activeSessionHolder)
            }
        })
        //This should be done as early as possible
        initKnownEmojiHashSet(appContext)
    }

    override fun providesMatrixConfiguration() = MatrixConfiguration(BuildConfig.FLAVOR_DESCRIPTION)

    override fun getWorkManagerConfiguration() = androidx.work.Configuration.Builder().build()

    override fun injector(): VectorComponent {
        return vectorComponent
    }

    private fun logInfo() {
        val appVersion = getVersion(longFormat = true, useBuildNumber = true)
        val sdkVersion = Matrix.getSdkVersion()
        val date = SimpleDateFormat("MM-dd HH:mm:ss.SSSZ", Locale.US).format(Date())

        Timber.v("----------------------------------------------------------------")
        Timber.v("----------------------------------------------------------------")
        Timber.v(" Application version: $appVersion")
        Timber.v(" SDK version: $sdkVersion")
        Timber.v(" Local time: $date")
        Timber.v("----------------------------------------------------------------")
        Timber.v("----------------------------------------------------------------\n\n\n\n")
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