/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceFragmentCompat
import com.airbnb.mvrx.MavericksView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.utils.toast
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import reactivecircus.flowbinding.android.view.clicks
import timber.log.Timber

abstract class VectorSettingsBaseFragment : PreferenceFragmentCompat(), MavericksView {
    /* ==========================================================================================
     * Analytics
     * ========================================================================================== */

    protected var analyticsScreenName: MobileScreen.ScreenName? = null

    protected lateinit var analyticsTracker: AnalyticsTracker

    /* ==========================================================================================
     * Activity
     * ========================================================================================== */

    val vectorActivity: VectorBaseActivity<*> by lazy {
        activity as VectorBaseActivity<*>
    }

    private var mLoadingView: View? = null

    // members
    protected lateinit var session: Session
    protected lateinit var errorFormatter: ErrorFormatter

    /* ==========================================================================================
     * ViewEvents
     * ========================================================================================== */

    protected fun <T : VectorViewEvents> VectorViewModel<*, *, T>.observeViewEvents(
            observer: (T) -> Unit,
    ) {
        val tag = this@VectorSettingsBaseFragment::class.simpleName.toString()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewEvents
                        .stream(tag)
                        .collect {
                            observer(it)
                        }
            }
        }
    }

    /* ==========================================================================================
     * Views
     * ========================================================================================== */

    protected fun View.debouncedClicks(onClicked: () -> Unit) {
        clicks()
                .onEach { onClicked() }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    abstract val preferenceXmlRes: Int

    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(preferenceXmlRes)
        bindPref()
    }

    override fun onAttach(context: Context) {
        val singletonEntryPoint = context.singletonEntryPoint()
        super.onAttach(context)
        session = singletonEntryPoint.activeSessionHolder().getActiveSession()
        errorFormatter = singletonEntryPoint.errorFormatter()
        analyticsTracker = singletonEntryPoint.analyticsTracker()
    }

    override fun onResume() {
        super.onResume()
        Timber.i("onResume Fragment ${javaClass.simpleName}")
        analyticsScreenName?.let {
            analyticsTracker.screen(MobileScreen(screenName = it))
        }
        vectorActivity.supportActionBar?.setTitle(titleRes)
        // find the view from parent activity
        mLoadingView = vectorActivity.findViewById(R.id.vector_settings_spinner_views)
    }

    abstract fun bindPref()

    abstract var titleRes: Int

    /* ==========================================================================================
     * Protected
     * ========================================================================================== */

    protected fun notImplemented() {
        // Snackbar cannot be display on PreferenceFragment. TODO It's maybe because the show() method is not used...
        // Snackbar.make(requireView(), CommonStrings.not_implemented, Snackbar.LENGTH_SHORT)
        activity?.toast(CommonStrings.not_implemented)
    }

    /**
     * Display the loading view.
     */
    protected fun displayLoadingView() {
        // search the loading view from the upper view
        if (null == mLoadingView) {
            var parent = view

            while (parent != null && mLoadingView == null) {
                mLoadingView = parent.findViewById(R.id.vector_settings_spinner_views)
                parent = parent.parent as View
            }
        } else {
            mLoadingView?.visibility = View.VISIBLE
        }
    }

    /**
     * Hide the loading view.
     */
    protected fun hideLoadingView() {
        mLoadingView?.visibility = View.GONE
    }

    /**
     * Hide the loading view and refresh the preferences.
     *
     * @param refresh true to refresh the display
     */
    protected fun hideLoadingView(refresh: Boolean) {
        mLoadingView?.visibility = View.GONE

        if (refresh) {
            // TODO refreshDisplay()
        }
    }

    protected fun displayErrorDialog(throwable: Throwable?) {
        displayErrorDialog(errorFormatter.toHumanReadable(throwable))
    }

    protected fun displayErrorDialog(errorMessage: String) {
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(CommonStrings.dialog_title_error)
                .setMessage(errorMessage)
                .setPositiveButton(CommonStrings.ok, null)
                .show()
    }

    override fun invalidate() {
        // No op by default
    }
}
