/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.platform

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.airbnb.mvrx.MavericksView
import dagger.hilt.android.EntryPointAccessors
import im.vector.app.core.di.ActivityEntryPoint
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.themes.ThemeUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import reactivecircus.flowbinding.android.view.clicks
import timber.log.Timber

/**
 * Add Mavericks capabilities, handle DI and bindings.
 */
abstract class VectorBaseDialogFragment<VB : ViewBinding> : DialogFragment(), MavericksView {
    /* ==========================================================================================
     * Analytics
     * ========================================================================================== */

    protected var analyticsScreenName: MobileScreen.ScreenName? = null

    protected lateinit var analyticsTracker: AnalyticsTracker

    /* ==========================================================================================
     * View
     * ========================================================================================== */

    private var _binding: VB? = null

    // This property is only valid between onCreateView and onDestroyView.
    protected val views: VB
        get() = _binding!!

    abstract fun getBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /* ==========================================================================================
     * View model
     * ========================================================================================== */

    private lateinit var viewModelFactory: ViewModelProvider.Factory

    protected val activityViewModelProvider
        get() = ViewModelProvider(requireActivity(), viewModelFactory)

    protected val fragmentViewModelProvider
        get() = ViewModelProvider(this, viewModelFactory)

    val vectorBaseActivity: VectorBaseActivity<*> by lazy {
        activity as VectorBaseActivity<*>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, ThemeUtils.getApplicationThemeRes(requireContext()))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = getBinding(inflater, container)
        return views.root
    }

    @CallSuper
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onAttach(context: Context) {
        val activityEntryPoint = EntryPointAccessors.fromActivity(vectorBaseActivity, ActivityEntryPoint::class.java)
        viewModelFactory = activityEntryPoint.viewModelFactory()
        val singletonEntryPoint = context.singletonEntryPoint()
        analyticsTracker = singletonEntryPoint.analyticsTracker()
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        Timber.i("onResume BottomSheet ${javaClass.simpleName}")
        analyticsScreenName?.let {
            analyticsTracker.screen(MobileScreen(screenName = it))
        }
    }

    override fun onStart() {
        super.onStart()
        // This ensures that invalidate() is called for static screens that don't
        // subscribe to a ViewModel.
        postInvalidate()
        requireDialog().window?.setWindowAnimations(androidx.appcompat.R.style.Animation_AppCompat_Dialog)
    }

    protected fun setArguments(args: Parcelable? = null) {
        arguments = args.toMvRxBundle()
    }

    /* ==========================================================================================
     * Views
     * ========================================================================================== */

    protected fun View.debouncedClicks(onClicked: () -> Unit) {
        clicks()
                .onEach { onClicked() }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    /* ==========================================================================================
     * ViewEvents
     * ========================================================================================== */

    protected fun <T : VectorViewEvents> VectorViewModel<*, *, T>.observeViewEvents(observer: (T) -> Unit) {
        val tag = this@VectorBaseDialogFragment::class.simpleName.toString()
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
}
