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
import im.vector.riotredesign.core.di.AppModule
import im.vector.riotredesign.core.services.AlarmSyncBroadcastReceiver
import im.vector.riotredesign.features.configuration.VectorConfiguration
import im.vector.riotredesign.features.crypto.keysbackup.KeysBackupModule
import im.vector.riotredesign.features.home.HomeModule
import im.vector.riotredesign.features.lifecycle.VectorActivityLifecycleCallbacks
import im.vector.riotredesign.features.notifications.NotificationDrawerManager
import im.vector.riotredesign.features.notifications.NotificationUtils
import im.vector.riotredesign.features.notifications.PushRuleTriggerListener
import im.vector.riotredesign.features.rageshake.VectorFileLogger
import im.vector.riotredesign.features.rageshake.VectorUncaughtExceptionHandler
import im.vector.riotredesign.features.roomdirectory.RoomDirectoryModule
import im.vector.riotredesign.features.settings.PreferencesManager
import im.vector.riotredesign.features.version.getVersion
import im.vector.riotredesign.push.fcm.FcmHelper
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.log.EmptyLogger
import org.koin.standalone.StandAloneContext.startKoin
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class VectorApplication : Application() {


    lateinit var appContext: Context
    //font thread handler
    private var mFontThreadHandler: Handler? = null

    val vectorConfiguration: VectorConfiguration by inject()

    private val notificationDrawerManager by inject<NotificationDrawerManager>()

//    var slowMode = false


    override fun onCreate() {
        super.onCreate()
        appContext = this

        logInfo()

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
        val keysBackupModule = KeysBackupModule().definition
        val koin = startKoin(listOf(appModule, homeModule, roomDirectoryModule, keysBackupModule), logger = EmptyLogger())
        Matrix.getInstance().setApplicationFlavor(BuildConfig.FLAVOR_DESCRIPTION)
        registerActivityLifecycleCallbacks(VectorActivityLifecycleCallbacks())

        val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs
        )

        FontsContractCompat.requestFont(this, fontRequest, koin.koinContext.get<EmojiCompatFontProvider>(), getFontThreadHandler())

        vectorConfiguration.initConfiguration()

        NotificationUtils.createNotificationChannels(applicationContext)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun entersForeground() {
                AlarmSyncBroadcastReceiver.cancelAlarm(appContext)
                Matrix.getInstance().currentSession?.also {
                    it.stopAnyBackgroundSync()
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun entersBackground() {
                Timber.i("App entered background") // call persistInfo

                notificationDrawerManager.persistInfo()

                if (FcmHelper.isPushSupported()) {
                    //TODO FCM fallback
                } else {
                    //TODO check if notifications are enabled for this device
                    //We need to use alarm in this mode
                    if (PreferencesManager.areNotificationEnabledForDevice(applicationContext)) {
                        if (Matrix.getInstance().currentSession != null) {
                            AlarmSyncBroadcastReceiver.scheduleAlarm(applicationContext, 4_000L)
                            Timber.i("Alarm scheduled to restart service")
                        }
                    }
                }
            }

        })


        Matrix.getInstance().currentSession?.let {
            it.refreshPushers()
            //bind to the sync service
            get<PushRuleTriggerListener>().startWithSession(it)
            it.fetchPushRules()
        }
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
        if (mFontThreadHandler == null) {
            val handlerThread = HandlerThread("fonts")
            handlerThread.start()
            mFontThreadHandler = Handler(handlerThread.looper)
        }
        return mFontThreadHandler!!
    }

}