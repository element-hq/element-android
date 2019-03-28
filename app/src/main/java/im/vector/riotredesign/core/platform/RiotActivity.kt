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

package im.vector.riotredesign.core.platform

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.*
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.airbnb.mvrx.BaseMvRxActivity
import com.bumptech.glide.util.Util
import im.vector.riotredesign.BuildConfig
import im.vector.riotredesign.R
import im.vector.riotredesign.features.rageshake.RageShake
import im.vector.riotredesign.receivers.DebugReceiver
import im.vector.ui.themes.ActivityOtherThemes
import im.vector.ui.themes.ThemeUtils
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable


abstract class RiotActivity : BaseMvRxActivity() {
    /* ==========================================================================================
     * UI
     * ========================================================================================== */

    @Nullable
    @BindView(R.id.toolbar)
    protected lateinit var toolbar: Toolbar

    /* ==========================================================================================
     * DATA
     * ========================================================================================== */

    private var unBinder: Unbinder? = null

    private var savedInstanceState: Bundle? = null

    // For debug only
    private var debugReceiver: DebugReceiver? = null

    private val uiDisposables = CompositeDisposable()
    private val restorables = ArrayList<Restorable>()

    private var rageShake: RageShake? = null

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
        super.onCreate(savedInstanceState)

        // Shake detector
        rageShake = RageShake(this)

        ThemeUtils.setActivityTheme(this, getOtherThemes())

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

    override fun onResume() {
        super.onResume()

        rageShake?.start()

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

        rageShake?.stop()

        debugReceiver?.let {
            unregisterReceiver(debugReceiver)
            debugReceiver = null
        }
    }

    /* ==========================================================================================
     * MENU MANAGEMENT
     * ========================================================================================== */

    final override fun onCreateOptionsMenu(menu: Menu): Boolean {
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
            setResult(RESULT_CANCELED)
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
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
     * Configure the Toolbar. It MUST be present in your layout with id "toolbar"
     */
    protected fun configureToolbar() {
        setSupportActionBar(toolbar)

        supportActionBar?.let {
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
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
    open fun getMenuTint() = 0 // TODO R.attr.vctr_icon_tint_on_dark_action_bar_color

    /**
     * Return a object containing other themes for this activity
     */
    open fun getOtherThemes(): ActivityOtherThemes = ActivityOtherThemes.Default
}