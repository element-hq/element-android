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

package im.vector.app.core.platform

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.annotation.AttrRes
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.annotation.MenuRes
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.airbnb.mvrx.MvRx
import com.bumptech.glide.util.Util
import com.google.android.material.snackbar.Snackbar
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.DaggerScreenComponent
import im.vector.app.core.di.HasScreenInjector
import im.vector.app.core.di.HasVectorInjector
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.di.VectorComponent
import im.vector.app.core.dialogs.DialogLocker
import im.vector.app.core.dialogs.UnrecognizedCertificateDialog
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.observeEvent
import im.vector.app.core.extensions.observeNotNull
import im.vector.app.core.extensions.vectorComponent
import im.vector.app.core.utils.toast
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.configuration.VectorConfiguration
import im.vector.app.features.consent.ConsentNotGivenHelper
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.pin.PinActivity
import im.vector.app.features.pin.PinLocker
import im.vector.app.features.pin.PinMode
import im.vector.app.features.pin.UnlockedActivity
import im.vector.app.features.rageshake.BugReportActivity
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.rageshake.RageShake
import im.vector.app.features.session.SessionListener
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.themes.ActivityOtherThemes
import im.vector.app.features.themes.ThemeUtils
import im.vector.app.receivers.DebugReceiver
import org.matrix.android.sdk.api.failure.GlobalError
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber
import kotlin.system.measureTimeMillis

abstract class VectorBaseActivity : AppCompatActivity(), HasScreenInjector {
    /* ==========================================================================================
     * UI
     * ========================================================================================== */

    @Nullable
    @JvmField
    @BindView(R.id.vector_coordinator_layout)
    var coordinatorLayout: CoordinatorLayout? = null

    /* ==========================================================================================
     * View model
     * ========================================================================================== */

    private lateinit var viewModelFactory: ViewModelProvider.Factory

    protected val viewModelProvider
        get() = ViewModelProvider(this, viewModelFactory)

    protected fun <T : VectorViewEvents> VectorViewModel<*, *, T>.observeViewEvents(observer: (T) -> Unit) {
        viewEvents
                .observe()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideWaitingView()
                    observer(it)
                }
                .disposeOnDestroy()
    }

    /* ==========================================================================================
     * DATA
     * ========================================================================================== */

    private lateinit var configurationViewModel: ConfigurationViewModel
    private lateinit var sessionListener: SessionListener
    protected lateinit var bugReporter: BugReporter
    private lateinit var pinLocker: PinLocker
    lateinit var rageShake: RageShake

    lateinit var navigator: Navigator
        private set
    private lateinit var fragmentFactory: FragmentFactory

    private lateinit var activeSessionHolder: ActiveSessionHolder
    private lateinit var vectorPreferences: VectorPreferences

    // Filter for multiple invalid token error
    private var mainActivityStarted = false

    private var unBinder: Unbinder? = null

    private var savedInstanceState: Bundle? = null

    // For debug only
    private var debugReceiver: DebugReceiver? = null

    private val uiDisposables = CompositeDisposable()
    private val restorables = ArrayList<Restorable>()

    private lateinit var screenComponent: ScreenComponent

    override fun attachBaseContext(base: Context) {
        val vectorConfiguration = VectorConfiguration(this)
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

    protected fun Disposable.disposeOnDestroy() {
        uiDisposables.add(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.i("onCreate Activity ${this.javaClass.simpleName}")
        val vectorComponent = getVectorComponent()
        screenComponent = DaggerScreenComponent.factory().create(vectorComponent, this)
        val timeForInjection = measureTimeMillis {
            injectWith(screenComponent)
        }
        Timber.v("Injecting dependencies into ${javaClass.simpleName} took $timeForInjection ms")
        ThemeUtils.setActivityTheme(this, getOtherThemes())
        fragmentFactory = screenComponent.fragmentFactory()
        supportFragmentManager.fragmentFactory = fragmentFactory
        super.onCreate(savedInstanceState)
        viewModelFactory = screenComponent.viewModelFactory()
        configurationViewModel = viewModelProvider.get(ConfigurationViewModel::class.java)
        bugReporter = screenComponent.bugReporter()
        pinLocker = screenComponent.pinLocker()
        // Shake detector
        rageShake = screenComponent.rageShake()
        navigator = screenComponent.navigator()
        activeSessionHolder = screenComponent.activeSessionHolder()
        vectorPreferences = vectorComponent.vectorPreferences()
        configurationViewModel.activityRestarter.observe(this, Observer {
            if (!it.hasBeenHandled) {
                // Recreate the Activity because configuration has changed
                startActivity(intent)
                finish()
            }
        })
        pinLocker.getLiveState().observeNotNull(this) {
            if (this@VectorBaseActivity !is UnlockedActivity && it == PinLocker.State.LOCKED) {
                navigator.openPinCode(this, PinMode.AUTH)
            }
        }
        sessionListener = vectorComponent.sessionListener()
        sessionListener.globalErrorLiveData.observeEvent(this) {
            handleGlobalError(it)
        }

        // Set flag FLAG_SECURE
        if (vectorPreferences.useFlagSecure()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        doBeforeSetContentView()

        if (getLayoutRes() != -1) {
            setContentView(getLayoutRes())
        }

        unBinder = ButterKnife.bind(this)

        this.savedInstanceState = savedInstanceState

        initUiAndData()

        val titleRes = getTitleRes()
        if (titleRes != -1) {
            supportActionBar?.let {
                it.setTitle(titleRes)
            } ?: run {
                setTitle(titleRes)
            }
        }
    }

    private fun handleGlobalError(globalError: GlobalError) {
        when (globalError) {
            is GlobalError.InvalidToken         ->
                handleInvalidToken(globalError)
            is GlobalError.ConsentNotGivenError ->
                consentNotGivenHelper.displayDialog(globalError.consentUri,
                        activeSessionHolder.getActiveSession().sessionParams.homeServerHost ?: "")
            is GlobalError.CertificateError     ->
                handleCertificateError(globalError)
        }.exhaustive
    }

    private fun handleCertificateError(certificateError: GlobalError.CertificateError) {
        vectorComponent()
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

        MainActivity.restartApp(this,
                MainActivityArgs(
                        clearCredentials = !globalError.softLogout,
                        isUserLoggedOut = true,
                        isSoftLogout = globalError.softLogout
                )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("onDestroy Activity ${this.javaClass.simpleName}")
        unBinder?.unbind()
        unBinder = null

        uiDisposables.dispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PinActivity.PIN_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK                 -> {
                    pinLocker.unlock()
                }
                else                               -> {
                    pinLocker.block()
                    moveTaskToBack(true)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.i("onResume Activity ${this.javaClass.simpleName}")

        configurationViewModel.onActivityResumed()

        if (this !is BugReportActivity && vectorPreferences.useRageshake()) {
            rageShake.start()
        }
        DebugReceiver
                .getIntentFilter(this)
                .takeIf { BuildConfig.DEBUG }
                ?.let {
                    debugReceiver = DebugReceiver()
                    registerReceiver(debugReceiver, it)
                }
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause Activity ${this.javaClass.simpleName}")

        rageShake.stop()

        debugReceiver?.let {
            unregisterReceiver(debugReceiver)
            debugReceiver = null
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus && displayInFullscreen()) {
            setFullScreen()
        }
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)

        Timber.w("onMultiWindowModeChanged. isInMultiWindowMode: $isInMultiWindowMode")
        bugReporter.inMultiWindowMode = isInMultiWindowMode
    }

    override fun injector(): ScreenComponent {
        return screenComponent
    }

    protected open fun injectWith(injector: ScreenComponent) = Unit

    protected fun createFragment(fragmentClass: Class<out Fragment>, args: Bundle?): Fragment {
        return fragmentFactory.instantiate(classLoader, fragmentClass.name).apply {
            arguments = args
        }
    }

    /* ==========================================================================================
     * PRIVATE METHODS
     * ========================================================================================== */

    internal fun getVectorComponent(): VectorComponent {
        return (application as HasVectorInjector).injector()
    }

    /**
     * Force to render the activity in fullscreen
     */
    private fun setFullScreen() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    /* ==========================================================================================
     * MENU MANAGEMENT
     * ========================================================================================== */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuRes = getMenuRes()

        if (menuRes != -1) {
            menuInflater.inflate(menuRes, menu)
            ThemeUtils.tintMenuIcons(menu, ThemeUtils.getColor(this, getMenuTint()))
            return true
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed(true)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        onBackPressed(false)
    }

    private fun onBackPressed(fromToolbar: Boolean) {
        val handled = recursivelyDispatchOnBackPressed(supportFragmentManager, fromToolbar)
        if (!handled) {
            super.onBackPressed()
        }
    }

    private fun recursivelyDispatchOnBackPressed(fm: FragmentManager, fromToolbar: Boolean): Boolean {
        val reverseOrder = fm.fragments.filterIsInstance<VectorBaseFragment>().reversed()
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
     * Is first creation
     *
     * @return true if Activity is created for the first time (and not restored by the system)
     */
    protected fun isFirstCreation() = savedInstanceState == null

    /**
     * Configure the Toolbar, with default back button.
     */
    protected fun configureToolbar(toolbar: Toolbar, displayBack: Boolean = true) {
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayShowHomeEnabled(displayBack)
            it.setDisplayHomeAsUpEnabled(displayBack)
            it.title = null
        }
    }

    fun Parcelable?.toMvRxBundle(): Bundle? {
        return this?.let { Bundle().apply { putParcelable(MvRx.KEY_ARG, it) } }
    }

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
     * Tells if the waiting view is currently displayed
     *
     * @return true if the waiting view is displayed
     */
    fun isWaitingViewVisible() = waitingView?.isVisible == true

    /**
     * Show the waiting view
     */
    open fun showWaitingView() {
        waitingView?.isVisible = true
    }

    /**
     * Hide the waiting view
     */
    open fun hideWaitingView() {
        waitingView?.isVisible = false
    }

    /* ==========================================================================================
     * OPEN METHODS
     * ========================================================================================== */

    @LayoutRes
    open fun getLayoutRes() = -1

    open fun displayInFullscreen() = false

    open fun doBeforeSetContentView() = Unit

    open fun initUiAndData() = Unit

    @StringRes
    open fun getTitleRes() = -1

    @MenuRes
    open fun getMenuRes() = -1

    @AttrRes
    open fun getMenuTint() = R.attr.vctr_icon_tint_on_light_action_bar_color

    /**
     * Return a object containing other themes for this activity
     */
    open fun getOtherThemes(): ActivityOtherThemes = ActivityOtherThemes.Default

    /* ==========================================================================================
     * PUBLIC METHODS
     * ========================================================================================== */

    fun showSnackbar(message: String) {
        coordinatorLayout?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

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
            toast(getString(R.string.not_implemented) + ": $message")
        } else {
            toast(getString(R.string.not_implemented))
        }
    }
}
