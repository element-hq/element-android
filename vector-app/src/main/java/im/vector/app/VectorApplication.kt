/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import android.util.Log
import android.view.Gravity
import androidx.core.content.ContextCompat
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDex
import androidx.recyclerview.widget.SnapHelper
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.EpoxyAsyncUtil
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Mavericks
import com.facebook.stetho.Stetho
import com.gabrielittner.threetenbp.LazyThreeTen
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import com.mapbox.mapboxsdk.Mapbox
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import dagger.hilt.android.HiltAndroidApp
import im.vector.app.config.Config
import im.vector.app.core.debug.FlipperProxy
import im.vector.app.core.debug.LeakDetector
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.pushers.FcmHelper
import im.vector.app.core.resources.BuildMeta
import im.vector.app.features.analytics.DecryptionFailureTracker
import im.vector.app.features.analytics.VectorAnalytics
import im.vector.app.features.analytics.plan.SuperProperties
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.configuration.VectorConfiguration
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
import im.vector.application.R
import org.jitsi.meet.sdk.log.JitsiMeetDefaultLogHandler
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.auth.AuthenticationService
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
    @Inject lateinit var spaceStateHandler: SpaceStateHandler
    @Inject lateinit var popupAlertManager: PopupAlertManager
    @Inject lateinit var pinLocker: PinLocker
    @Inject lateinit var callManager: WebRtcCallManager
    @Inject lateinit var invitesAcceptor: InvitesAcceptor
    @Inject lateinit var autoRageShaker: AutoRageShaker
    @Inject lateinit var decryptionFailureTracker: DecryptionFailureTracker
    @Inject lateinit var vectorFileLogger: VectorFileLogger
    @Inject lateinit var vectorAnalytics: VectorAnalytics
    @Inject lateinit var flipperProxy: FlipperProxy
    @Inject lateinit var matrix: Matrix
    @Inject lateinit var fcmHelper: FcmHelper
    @Inject lateinit var buildMeta: BuildMeta
    @Inject lateinit var leakDetector: LeakDetector
    @Inject lateinit var vectorLocale: VectorLocale
    @Inject lateinit var webRtcCallManager: WebRtcCallManager

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
        flipperProxy.init(matrix)
        vectorAnalytics.init()
        vectorAnalytics.updateSuperProperties(
                SuperProperties(
                        appPlatform = SuperProperties.AppPlatform.EA,
                        cryptoSDK = SuperProperties.CryptoSDK.Rust,
                        cryptoSDKVersion = Matrix.getCryptoVersion(longFormat = false)
                )
        )
        invitesAcceptor.initialize()
        autoRageShaker.initialize()
        decryptionFailureTracker.start()
        vectorUncaughtExceptionHandler.activate()

        // Remove Log handler statically added by Jitsi
        Timber.forest()
                .filterIsInstance(JitsiMeetDefaultLogHandler::class.java)
                .forEach { Timber.uproot(it) }

        if (buildMeta.isDebug) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(vectorFileLogger)

        if (buildMeta.isDebug) {
            Stetho.initializeWithDefaults(this)
        }
        logInfo()
        LazyThreeTen.init(this)
        Mavericks.initialize(debugMode = false)

        configureEpoxy()

        registerActivityLifecycleCallbacks(VectorActivityLifecycleCallbacks(popupAlertManager))
        val fontRequest = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs
        )
        FontsContractCompat.requestFont(this, fontRequest, emojiCompatFontProvider, getFontThreadHandler())
        vectorLocale.init()
        ThemeUtils.init(this)
        vectorConfiguration.applyToApplicationContext()

        emojiCompatWrapper.init(fontRequest)

        notificationUtils.createNotificationChannels()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            private var stopBackgroundSync = false

            override fun onResume(owner: LifecycleOwner) {
                Timber.i("App entered foreground")
                fcmHelper.onEnterForeground(activeSessionHolder)
                if (webRtcCallManager.currentCall.get() == null) {
                    Timber.i("App entered foreground and no active call: stop any background sync")
                    activeSessionHolder.getSafeActiveSessionAsync {
                        it?.syncService()?.stopAnyBackgroundSync()
                    }
                } else {
                    Timber.i("App entered foreground: there is an active call, set stopBackgroundSync to true")
                    stopBackgroundSync = true
                }
            }

            override fun onPause(owner: LifecycleOwner) {
                Timber.i("App entered background")
                fcmHelper.onEnterBackground(activeSessionHolder)

                if (stopBackgroundSync) {
                    if (webRtcCallManager.currentCall.get() == null) {
                        Timber.i("App entered background: stop any background sync")
                        activeSessionHolder.getSafeActiveSessionAsync {
                            it?.syncService()?.stopAnyBackgroundSync()
                        }
                        stopBackgroundSync = false
                    } else {
                        Timber.i("App entered background: there is an active call do not stop background sync")
                    }
                }
            }
        })
        ProcessLifecycleOwner.get().lifecycle.addObserver(spaceStateHandler)
        ProcessLifecycleOwner.get().lifecycle.addObserver(pinLocker)
        ProcessLifecycleOwner.get().lifecycle.addObserver(callManager)
        // This should be done as early as possible
        // initKnownEmojiHashSet(appContext)
        ContextCompat.registerReceiver(
                applicationContext,
                powerKeyReceiver,
                IntentFilter().apply {
                    // Looks like i cannot receive OFF, if i don't have both ON and OFF
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        EmojiManager.install(GoogleEmojiProvider())

        // Initialize Mapbox before inflating mapViews
        Mapbox.getInstance(this)

        initMemoryLeakAnalysis()
    }

    private fun configureEpoxy() {
        EpoxyController.defaultDiffingHandler = EpoxyAsyncUtil.getAsyncBackgroundHandler()
        EpoxyController.defaultModelBuildingHandler = EpoxyAsyncUtil.getAsyncBackgroundHandler()
        Carousel.setDefaultGlobalSnapHelperFactory(object : Carousel.SnapHelperFactory() {
            override fun buildSnapHelper(context: Context?): SnapHelper {
                return GravitySnapHelper(Gravity.START)
            }
        })
    }

    private fun enableStrictModeIfNeeded() {
        if (Config.ENABLE_STRICT_MODE_LOGS) {
            StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyLog()
                            .build()
            )
        }
    }

    override fun getWorkManagerConfiguration(): WorkConfiguration {
        return WorkConfiguration.Builder()
                .setWorkerFactory(matrix.getWorkerFactory())
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(Executors.newCachedThreadPool())
                .build()
    }

    private fun logInfo() {
        val appVersion = versionProvider.getVersion(longFormat = true)
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
        val handlerThread = HandlerThread("Vector-fonts")
        handlerThread.start()
        return Handler(handlerThread.looper)
    }

    private fun initMemoryLeakAnalysis() {
        leakDetector.enable(vectorPreferences.isMemoryLeakAnalysisEnabled())
    }
}
