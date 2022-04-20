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

package im.vector.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Handler
import android.os.HandlerThread
import android.os.StrictMode
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Mavericks
import com.facebook.stetho.Stetho
import com.gabrielittner.threetenbp.LazyThreeTen
import com.mapbox.mapboxsdk.Mapbox
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import dagger.hilt.android.HiltAndroidApp
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.extensions.configureAndStart
import im.vector.app.core.extensions.startSyncing
import im.vector.app.features.analytics.VectorAnalytics
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.configuration.VectorConfiguration
import im.vector.app.features.disclaimer.doNotShowDisclaimerDialog
import im.vector.app.features.invite.InvitesAcceptor
import im.vector.app.features.lifecycle.VectorActivityLifecycleCallbacks
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.pin.PinLocker
import im.vector.app.features.popup.PopupAlertManager
import im.vector.app.features.rageshake.VectorFileLogger
import im.vector.app.features.rageshake.VectorUncaughtExceptionHandler
import im.vector.app.features.settings.VectorLocale
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.themes.ThemeUtils
import im.vector.app.features.version.VersionProvider
import im.vector.app.push.fcm.FcmHelper
import org.jitsi.meet.sdk.log.JitsiMeetDefaultLogHandler
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.legacy.LegacySessionImporter
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import javax.inject.Inject
import androidx.work.Configuration as WorkConfiguration

@HiltAndroidApp
class VectorApplication :
        Application(),
        WorkConfiguration.Provider {

    lateinit var appContext: Context
    @Inject lateinit var legacySessionImporter: LegacySessionImporter
    @Inject lateinit var authenticationService: AuthenticationService
    @Inject lateinit var vectorConfiguration: VectorConfiguration
    @Inject lateinit var emojiCompatFontProvider: EmojiCompatFontProvider
    @Inject lateinit var emojiCompatWrapper: EmojiCompatWrapper
    @Inject lateinit var vectorUncaughtExceptionHandler: VectorUncaughtExceptionHandler
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var versionProvider: VersionProvider
    @Inject lateinit var notificationUtils: NotificationUtils
    @Inject lateinit var appStateHandler: AppStateHandler
    @Inject lateinit var popupAlertManager: PopupAlertManager
    @Inject lateinit var pinLocker: PinLocker
    @Inject lateinit var callManager: WebRtcCallManager
    @Inject lateinit var invitesAcceptor: InvitesAcceptor
    @Inject lateinit var autoRageShaker: AutoRageShaker
    @Inject lateinit var vectorFileLogger: VectorFileLogger
    @Inject lateinit var vectorAnalytics: VectorAnalytics
    @Inject lateinit var matrix: Matrix

    // font thread handler
    private var fontThreadHandler: Handler? = null

    private val powerKeyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF &&
                    vectorPreferences.useFlagPinCode()) {
                pinLocker.screenIsOff()
            }
        }
    }

    override fun onCreate() {
        enableStrictModeIfNeeded()
        super.onCreate()
        appContext = this
        vectorAnalytics.init()
        invitesAcceptor.initialize()
        autoRageShaker.initialize()
        vectorUncaughtExceptionHandler.activate()

        // Remove Log handler statically added by Jitsi
        Timber.forest()
                .filterIsInstance(JitsiMeetDefaultLogHandler::class.java)
                .forEach { Timber.uproot(it) }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(vectorFileLogger)

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }
        logInfo()
        LazyThreeTen.init(this)
        Mavericks.initialize(debugMode = false)
        EpoxyController.defaultDiffingHandler = EpoxyAsyncUtil.getAsyncBackgroundHandler()
        EpoxyController.defaultModelBuildingHandler = EpoxyAsyncUtil.getAsyncBackgroundHandler()
        registerActivityLifecycleCallbacks(VectorActivityLifecycleCallbacks(popupAlertManager))
        val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs
        )
        FontsContractCompat.requestFont(this, fontRequest, emojiCompatFontProvider, getFontThreadHandler())
        VectorLocale.init(this)
        ThemeUtils.init(this)
        vectorConfiguration.applyToApplicationContext()

        emojiCompatWrapper.init(fontRequest)

        notificationUtils.createNotificationChannels()

        // It can takes time, but do we care?
        val sessionImported = legacySessionImporter.process()
        if (!sessionImported) {
            // Do not display the name change popup
            doNotShowDisclaimerDialog(this)
        }

        if (authenticationService.hasAuthenticatedSessions() && !activeSessionHolder.hasActiveSession()) {
            val lastAuthenticatedSession = authenticationService.getLastAuthenticatedSession()!!
            activeSessionHolder.setActiveSession(lastAuthenticatedSession)
            lastAuthenticatedSession.configureAndStart(applicationContext, startSyncing = false)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(startSyncOnFirstStart)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                Timber.i("App entered foreground")
                FcmHelper.onEnterForeground(appContext, activeSessionHolder)
                activeSessionHolder.getSafeActiveSession()?.also {
                    it.stopAnyBackgroundSync()
                }
            }

            override fun onPause(owner: LifecycleOwner) {
                Timber.i("App entered background")
                FcmHelper.onEnterBackground(appContext, vectorPreferences, activeSessionHolder)
            }
        })
        ProcessLifecycleOwner.get().lifecycle.addObserver(appStateHandler)
        ProcessLifecycleOwner.get().lifecycle.addObserver(pinLocker)
        ProcessLifecycleOwner.get().lifecycle.addObserver(callManager)
        // This should be done as early as possible
        // initKnownEmojiHashSet(appContext)

        applicationContext.registerReceiver(powerKeyReceiver, IntentFilter().apply {
            // Looks like i cannot receive OFF, if i don't have both ON and OFF
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        })

        EmojiManager.install(GoogleEmojiProvider())

        // Initialize Mapbox before inflating mapViews
        Mapbox.getInstance(this)
    }

    private val startSyncOnFirstStart = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            Timber.i("App process started")
            authenticationService.getLastAuthenticatedSession()?.startSyncing(appContext)
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        }
    }

    private fun enableStrictModeIfNeeded() {
        if (BuildConfig.ENABLE_STRICT_MODE_LOGS) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build())
        }
    }

    override fun getWorkManagerConfiguration(): WorkConfiguration {
        return WorkConfiguration.Builder()
                .setWorkerFactory(matrix.workerFactory())
                .setExecutor(Executors.newCachedThreadPool())
                .build()
    }

    private fun logInfo() {
        val appVersion = versionProvider.getVersion(longFormat = true, useBuildNumber = true)
        val sdkVersion = Matrix.getSdkVersion()
        val date = SimpleDateFormat("MM-dd HH:mm:ss.SSSZ", Locale.US).format(Date())

        Timber.d("----------------------------------------------------------------")
        Timber.d("----------------------------------------------------------------")
        Timber.d(" Application version: $appVersion")
        Timber.d(" SDK version: $sdkVersion")
        Timber.d(" Local time: $date")
        Timber.d("----------------------------------------------------------------")
        Timber.d("----------------------------------------------------------------\n\n\n\n")
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        vectorConfiguration.onConfigurationChanged()
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
