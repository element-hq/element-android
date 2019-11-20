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

package im.vector.riotx.core.platform

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.airbnb.mvrx.MvRx
import com.bumptech.glide.util.Util
import com.google.android.material.snackbar.Snackbar
import im.vector.riotx.BuildConfig
import im.vector.riotx.R
import im.vector.riotx.core.di.*
import im.vector.riotx.core.dialogs.DialogLocker
import im.vector.riotx.core.extensions.observeEvent
import im.vector.riotx.core.utils.toast
import im.vector.riotx.features.configuration.VectorConfiguration
import im.vector.riotx.features.consent.ConsentNotGivenHelper
import im.vector.riotx.features.navigation.Navigator
import im.vector.riotx.features.rageshake.BugReportActivity
import im.vector.riotx.features.rageshake.BugReporter
import im.vector.riotx.features.rageshake.RageShake
import im.vector.riotx.features.session.SessionListener
import im.vector.riotx.features.themes.ActivityOtherThemes
import im.vector.riotx.features.themes.ThemeUtils
import im.vector.riotx.receivers.DebugReceiver
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
        get() = ViewModelProviders.of(this, viewModelFactory)

    /* ==========================================================================================
     * DATA
     * ========================================================================================== */

    private lateinit var configurationViewModel: ConfigurationViewModel
    private lateinit var sessionListener: SessionListener
    protected lateinit var bugReporter: BugReporter
    private lateinit var rageShake: RageShake
    protected lateinit var navigator: Navigator
    private lateinit var activeSessionHolder: ActiveSessionHolder

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

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        restorables.forEach { it.onRestoreInstanceState(savedInstanceState) }
        super.onRestoreInstanceState(savedInstanceState)
    }

    @MainThread
    protected fun <T : Restorable> T.register(): T {
        Util.assertMainThread()
        restorables.add(this)
        return this
    }

    protected fun Disposable.disposeOnDestroy(): Disposable {
        uiDisposables.add(this)
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        screenComponent = DaggerScreenComponent.factory().create(getVectorComponent(), this)
        val timeForInjection = measureTimeMillis {
            injectWith(screenComponent)
        }
        Timber.v("Injecting dependencies into ${javaClass.simpleName} took $timeForInjection ms")
        ThemeUtils.setActivityTheme(this, getOtherThemes())
        supportFragmentManager.fragmentFactory = screenComponent.fragmentFactory()
        super.onCreate(savedInstanceState)
        viewModelFactory = screenComponent.viewModelFactory()
        configurationViewModel = viewModelProvider.get(ConfigurationViewModel::class.java)
        bugReporter = screenComponent.bugReporter()
        // Shake detector
        rageShake = screenComponent.rageShake()
        navigator = screenComponent.navigator()
        activeSessionHolder = screenComponent.activeSessionHolder()
        configurationViewModel.activityRestarter.observe(this, Observer {
            if (!it.hasBeenHandled) {
                // Recreate the Activity because configuration has changed
                startActivity(intent)
                finish()
            }
        })

        sessionListener = getVectorComponent().sessionListener()
        sessionListener.consentNotGivenLiveData.observeEvent(this) {
            consentNotGivenHelper.displayDialog(it.consentUri,
                    activeSessionHolder.getActiveSession().sessionParams.homeServerConnectionConfig.homeServerUri.host ?: "")
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

    override fun onDestroy() {
        super.onDestroy()
        unBinder?.unbind()
        unBinder = null

        uiDisposables.dispose()
    }

    override fun onResume() {
        super.onResume()

        Timber.v("onResume Activity ${this.javaClass.simpleName}")

        configurationViewModel.onActivityResumed()

        if (this !is BugReportActivity) {
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

        if (displayBack) {
            supportActionBar?.let {
                it.setDisplayShowHomeEnabled(true)
                it.setDisplayHomeAsUpEnabled(true)
            }
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
