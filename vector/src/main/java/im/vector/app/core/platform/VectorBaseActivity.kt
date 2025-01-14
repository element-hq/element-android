/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.MultiWindowModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.viewbinding.ViewBinding
import com.airbnb.mvrx.MavericksView
import com.bumptech.glide.util.Util
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.EntryPointAccessors
import im.vector.app.R
import im.vector.app.core.debug.DebugReceiver
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.ActivityEntryPoint
import im.vector.app.core.dialogs.DialogLocker
import im.vector.app.core.dialogs.UnrecognizedCertificateDialog
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.error.fatalError
import im.vector.app.core.extensions.observeEvent
import im.vector.app.core.extensions.observeNotNull
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.restart
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.utils.AndroidSystemSettingsProvider
import im.vector.app.core.utils.ToolbarConfig
import im.vector.app.core.utils.toast
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.VectorFeatures
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.configuration.VectorConfiguration
import im.vector.app.features.consent.ConsentNotGivenHelper
import im.vector.app.features.mdm.MdmService
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.pin.PinLocker
import im.vector.app.features.pin.PinMode
import im.vector.app.features.pin.UnlockedActivity
import im.vector.app.features.rageshake.BugReportActivity
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.rageshake.RageShake
import im.vector.app.features.session.SessionListener
import im.vector.app.features.settings.FontScalePreferences
import im.vector.app.features.settings.FontScalePreferencesImpl
import im.vector.app.features.settings.VectorLocaleProvider
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.themes.ActivityOtherThemes
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.failure.InitialSyncRequestReason
import reactivecircus.flowbinding.android.view.clicks
import timber.log.Timber
import javax.inject.Inject

abstract class VectorBaseActivity<VB : ViewBinding> : AppCompatActivity(), MavericksView {
    /* ==========================================================================================
     * Analytics
     * ========================================================================================== */

    protected var analyticsScreenName: MobileScreen.ScreenName? = null

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    /* ==========================================================================================
     * View
     * ========================================================================================== */

    protected lateinit var views: VB

    /* ==========================================================================================
     * View model
     * ========================================================================================== */

    private lateinit var viewModelFactory: ViewModelProvider.Factory

    protected val viewModelProvider
        get() = ViewModelProvider(this, viewModelFactory)

    fun <T : VectorViewEvents> VectorViewModel<*, *, T>.observeViewEvents(
            observer: (T) -> Unit,
    ) {
        val tag = this@VectorBaseActivity::class.simpleName.toString()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewEvents
                        .stream(tag)
                        .collect {
                            hideWaitingView()
                            observer(it)
                        }
            }
        }
    }

    var toolbar: ToolbarConfig? = null

    /* ==========================================================================================
     * Views
     * ========================================================================================== */

    protected fun View.debouncedClicks(onClicked: () -> Unit) {
        clicks()
                .onEach { onClicked() }
                .launchIn(lifecycleScope)
    }

    /* ==========================================================================================
     * DATA
     * ========================================================================================== */

    private lateinit var configurationViewModel: ConfigurationViewModel

    @Inject lateinit var sessionListener: SessionListener
    @Inject lateinit var bugReporter: BugReporter
    @Inject lateinit var pinLocker: PinLocker
    @Inject lateinit var rageShake: RageShake
    @Inject lateinit var buildMeta: BuildMeta
    @Inject lateinit var fontScalePreferences: FontScalePreferences
    @Inject lateinit var vectorLocale: VectorLocaleProvider
    @Inject lateinit var vectorFeatures: VectorFeatures
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var errorFormatter: ErrorFormatter
    @Inject lateinit var mdmService: MdmService

    // For debug only
    @Inject lateinit var debugReceiver: DebugReceiver

    // Filter for multiple invalid token error
    private var mainActivityStarted = false

    private var savedInstanceState: Bundle? = null

    private val restorables = ArrayList<Restorable>()

    override fun attachBaseContext(base: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(base)
        val fontScalePreferences = FontScalePreferencesImpl(preferences, AndroidSystemSettingsProvider(base))
        val vectorLocaleProvider = VectorLocaleProvider(preferences)
        val vectorConfiguration = VectorConfiguration(this, fontScalePreferences, vectorLocaleProvider)
        super.attachBaseContext(vectorConfiguration.getLocalisedContext(base))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        restorables.forEach { it.onSaveInstanceState(outState) }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        restorables.forEach { it.onRestoreInstanceState(savedInstanceState) }
        super.onRestoreInstanceState(savedInstanceState)
    }

    @MainThread
    protected fun <T : Restorable> T.register(): T {
        Util.assertMainThread()
        restorables.add(this)
        return this
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.i("onCreate Activity ${javaClass.simpleName}")
        val activityEntryPoint = EntryPointAccessors.fromActivity(this, ActivityEntryPoint::class.java)
        ThemeUtils.setActivityTheme(this, getOtherThemes())
        viewModelFactory = activityEntryPoint.viewModelFactory()
        super.onCreate(savedInstanceState)
        addOnMultiWindowModeChangedListener(onMultiWindowModeChangedListener)
        setupMenu()
        configurationViewModel = viewModelProvider.get(ConfigurationViewModel::class.java)
        configurationViewModel.activityRestarter.observe(this) {
            if (!it.hasBeenHandled) {
                // Recreate the Activity because configuration has changed
                restart()
            }
        }
        pinLocker.getLiveState().observeNotNull(this) {
            if (this@VectorBaseActivity !is UnlockedActivity && it == PinLocker.State.LOCKED) {
                navigator.openPinCode(this, pinStartForActivityResult, PinMode.AUTH)
            }
        }
        sessionListener.globalErrorLiveData.observeEvent(this) {
            handleGlobalError(it)
        }

        // Set flag FLAG_SECURE
        if (vectorPreferences.useFlagSecure()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        doBeforeSetContentView()

        // Hack for font size
        applyFontSize()

        views = getBinding()
        setContentView(views.root)

        this.savedInstanceState = savedInstanceState

        initUiAndData()

        if (vectorPreferences.isNewAppLayoutEnabled()) {
            tryOrNull { // Add to XML theme when feature flag is removed
                val toolbarBackground = MaterialColors.getColor(views.root, im.vector.lib.ui.styles.R.attr.vctr_toolbar_background)
                window.statusBarColor = toolbarBackground
                window.navigationBarColor = toolbarBackground
            }
        }

        val titleRes = getTitleRes()
        if (titleRes != -1) {
            supportActionBar?.let {
                it.setTitle(titleRes)
            } ?: run {
                setTitle(titleRes)
            }
        }
    }

    private fun setupMenu() {
        // Always add a MenuProvider to handle the back action from the Toolbar
        val vectorMenuProvider = this as? VectorMenuProvider
        addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        vectorMenuProvider?.let {
                            menuInflater.inflate(it.getMenuRes(), menu)
                            it.handlePostCreateMenu(menu)
                        }
                    }

                    override fun onPrepareMenu(menu: Menu) {
                        vectorMenuProvider?.handlePrepareMenu(menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                        return vectorMenuProvider?.handleMenuItemSelected(menuItem).orFalse() ||
                                handleMenuItemHome(menuItem)
                    }
                },
                this,
                Lifecycle.State.RESUMED
        )
    }

    /**
     * This method has to be called for the font size setting be supported correctly.
     */
    private fun applyFontSize() {
        resources.configuration.fontScale = fontScalePreferences.getResolvedFontScaleValue().scale

        @Suppress("DEPRECATION")
        resources.updateConfiguration(resources.configuration, resources.displayMetrics)
    }

    private fun handleGlobalError(globalError: GlobalError) {
        when (globalError) {
            is GlobalError.InvalidToken -> handleInvalidToken(globalError)
            is GlobalError.ConsentNotGivenError -> displayConsentNotGivenDialog(globalError)
            is GlobalError.CertificateError -> handleCertificateError(globalError)
            GlobalError.ExpiredAccount -> Unit // TODO Handle account expiration
            is GlobalError.InitialSyncRequest -> handleInitialSyncRequest(globalError)
        }
    }

    private fun displayConsentNotGivenDialog(globalError: GlobalError.ConsentNotGivenError) {
        consentNotGivenHelper.displayDialog(globalError.consentUri, activeSessionHolder.getActiveSession().sessionParams.homeServerHost ?: "")
    }

    private fun handleInitialSyncRequest(initialSyncRequest: GlobalError.InitialSyncRequest) {
        MaterialAlertDialogBuilder(this)
                .setTitle(CommonStrings.initial_sync_request_title)
                .setMessage(
                        getString(
                                CommonStrings.initial_sync_request_content, getString(
                                when (initialSyncRequest.reason) {
                                    InitialSyncRequestReason.IGNORED_USERS_LIST_CHANGE -> CommonStrings.initial_sync_request_reason_unignored_users
                                }
                        )
                        )
                )
                .setPositiveButton(CommonStrings.ok) { _, _ ->
                    MainActivity.restartApp(this, MainActivityArgs(clearCache = true))
                }
                .setNegativeButton(CommonStrings.later, null)
                .show()
    }

    private fun handleCertificateError(certificateError: GlobalError.CertificateError) {
        singletonEntryPoint()
                .unrecognizedCertificateDialog()
                .show(this,
                        certificateError.fingerprint,
                        object : UnrecognizedCertificateDialog.Callback {
                            override fun onAccept() {
                                // TODO Support certificate error once logged
                            }

                            override fun onIgnore() {
                                // TODO Support certificate error once logged
                            }

                            override fun onReject() {
                                // TODO Support certificate error once logged
                            }
                        }
                )
    }

    protected open fun handleInvalidToken(globalError: GlobalError.InvalidToken) {
        Timber.w("Invalid token event received")
        if (mainActivityStarted) {
            return
        }

        mainActivityStarted = true

        MainActivity.restartApp(
                this,
                MainActivityArgs(
                        clearCredentials = !globalError.softLogout,
                        isUserLoggedOut = true,
                        isSoftLogout = globalError.softLogout
                )
        )
    }

    override fun onDestroy() {
        removeOnMultiWindowModeChangedListener(onMultiWindowModeChangedListener)
        super.onDestroy()
        Timber.i("onDestroy Activity ${javaClass.simpleName}")
    }

    private val pinStartForActivityResult = registerStartForActivityResult { activityResult ->
        when (activityResult.resultCode) {
            Activity.RESULT_OK -> {
                Timber.v("Pin ok, unlock app")
                pinLocker.unlock()

                // Cancel any new started PinActivity, after a screen rotation for instance
                // FIXME I cannot use this anymore :/
                // finishActivity(PinActivity.PIN_REQUEST_CODE)
            }
            else -> {
                if (pinLocker.getLiveState().value != PinLocker.State.UNLOCKED) {
                    // Remove the task, to be sure that PIN code will be requested when resumed
                    finishAndRemoveTask()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.i("onResume Activity ${javaClass.simpleName}")
        analyticsScreenName?.let {
            analyticsTracker.screen(MobileScreen(screenName = it))
        }
        configurationViewModel.onActivityResumed()

        if (this !is BugReportActivity && vectorPreferences.useRageshake()) {
            rageShake.start()
        }
        debugReceiver.register(this)
        mdmService.registerListener(this) {
            // Just log that a change occurred.
            Timber.w("MDM data has been updated")
        }
    }

    private val postResumeScheduledActions = mutableListOf<() -> Unit>()

    /**
     * Schedule action to be done in the next call of onPostResume().
     * It fixes bug observed on Android 6 (API 23).
     */
    protected fun doOnPostResume(action: () -> Unit) {
        synchronized(postResumeScheduledActions) {
            postResumeScheduledActions.add(action)
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        synchronized(postResumeScheduledActions) {
            postResumeScheduledActions.forEach {
                tryOrNull { it.invoke() }
            }
            postResumeScheduledActions.clear()
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause Activity ${javaClass.simpleName}")

        rageShake.stop()
        debugReceiver.unregister(this)
        mdmService.unregisterListener(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus && displayInFullscreen()) {
            setFullScreen()
        }
    }

    private val onMultiWindowModeChangedListener = Consumer<MultiWindowModeChangedInfo> {
        Timber.w("onMultiWindowModeChanged. isInMultiWindowMode: ${it.isInMultiWindowMode}")
        bugReporter.inMultiWindowMode = it.isInMultiWindowMode
    }

    /* ==========================================================================================
     * PRIVATE METHODS
     * ========================================================================================== */

    /**
     * Force to render the activity in fullscreen.
     */
    private fun setFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // New API instead of SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN and SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            window.setDecorFitsSystemWindows(false)
            // New API instead of SYSTEM_UI_FLAG_IMMERSIVE
            window.decorView.windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // New API instead of FLAG_TRANSLUCENT_STATUS
            window.statusBarColor = ContextCompat.getColor(this, im.vector.lib.ui.styles.R.color.half_transparent_status_bar)
            // New API instead of FLAG_TRANSLUCENT_NAVIGATION
            window.navigationBarColor = ContextCompat.getColor(this, im.vector.lib.ui.styles.R.color.half_transparent_status_bar)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private fun handleMenuItemHome(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed(true)
                true
            }
            else -> false
        }
    }

    @SuppressLint("MissingSuperCall")
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        onBackPressed(false)
    }

    private fun onBackPressed(fromToolbar: Boolean) {
        val handled = recursivelyDispatchOnBackPressed(supportFragmentManager, fromToolbar)
        if (!handled) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun recursivelyDispatchOnBackPressed(fm: FragmentManager, fromToolbar: Boolean): Boolean {
        val reverseOrder = fm.fragments.filterIsInstance<VectorBaseFragment<*>>().reversed()
        for (f in reverseOrder) {
            val handledByChildFragments = recursivelyDispatchOnBackPressed(f.childFragmentManager, fromToolbar)
            if (handledByChildFragments) {
                return true
            }
            if (f is OnBackPressed && f.onBackPressed(fromToolbar)) {
                return true
            }
        }
        return false
    }

    /* ==========================================================================================
     * PROTECTED METHODS
     * ========================================================================================== */

    /**
     * Get the saved instance state.
     * Ensure {@link isFirstCreation()} returns false before calling this
     *
     * @return
     */
    protected fun getSavedInstanceState(): Bundle {
        return savedInstanceState!!
    }

    /**
     * Is first creation.
     *
     * @return true if Activity is created for the first time (and not restored by the system)
     */
    protected fun isFirstCreation() = savedInstanceState == null

    // ==============================================================================================
    // Handle loading view (also called waiting view or spinner view)
    // ==============================================================================================

    var waitingView: View? = null
        set(value) {
            field = value

            // Ensure this view is clickable to catch UI events
            value?.isClickable = true
        }

    /**
     * Tells if the waiting view is currently displayed.
     *
     * @return true if the waiting view is displayed
     */
    fun isWaitingViewVisible() = waitingView?.isVisible == true

    /**
     * Show the waiting view, and set text if not null.
     */
    open fun showWaitingView(text: String? = null) {
        waitingView?.isVisible = true
        if (text != null) {
            waitingView?.findViewById<TextView>(R.id.waitingStatusText)?.setTextOrHide(text)
        }
    }

    /**
     * Hide the waiting view.
     */
    open fun hideWaitingView() {
        waitingView?.isVisible = false
    }

    /* ==========================================================================================
     * OPEN METHODS
     * ========================================================================================== */

    abstract fun getBinding(): VB

    open fun displayInFullscreen() = false

    open fun doBeforeSetContentView() = Unit

    open fun initUiAndData() = Unit

    // Note: does not seem to be called
    final override fun invalidate() = Unit

    @StringRes
    open fun getTitleRes() = -1

    /**
     * Return a object containing other themes for this activity.
     */
    open fun getOtherThemes(): ActivityOtherThemes = ActivityOtherThemes.Default

    /* ==========================================================================================
     * PUBLIC METHODS
     * ========================================================================================== */

    fun showSnackbar(message: String) {
        getCoordinatorLayout()?.showOptimizedSnackbar(message)
    }

    fun showSnackbar(message: String, @StringRes withActionTitle: Int?, action: (() -> Unit)?) {
        val coordinatorLayout = getCoordinatorLayout()
        if (coordinatorLayout != null) {
            Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG).apply {
                withActionTitle?.let {
                    setAction(withActionTitle) { action?.invoke() }
                }
            }.show()
        } else {
            fatalError("No CoordinatorLayout to display this snackbar!", vectorPreferences.failFast())
        }
    }

    open fun getCoordinatorLayout(): CoordinatorLayout? = null

    /* ==========================================================================================
     * User Consent
     * ========================================================================================== */

    private val consentNotGivenHelper by lazy {
        ConsentNotGivenHelper(this, DialogLocker(savedInstanceState))
                .apply { restorables.add(this) }
    }

    /* ==========================================================================================
     * Temporary method
     * ========================================================================================== */

    fun notImplemented(message: String = "") {
        if (message.isNotBlank()) {
            toast(getString(CommonStrings.not_implemented) + ": $message")
        } else {
            toast(getString(CommonStrings.not_implemented))
        }
    }

    /**
     * Sets toolbar as actionBar.
     *
     * @return Instance of [ToolbarConfig] with set of helper methods to configure toolbar
     * */
    fun setupToolbar(toolbar: MaterialToolbar) = ToolbarConfig(this, toolbar).also {
        this.toolbar = it.setup()
    }
}
