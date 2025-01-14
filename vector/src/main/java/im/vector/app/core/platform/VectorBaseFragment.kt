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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.airbnb.mvrx.MavericksView
import com.bumptech.glide.util.Util.assertMainThread
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.EntryPointAccessors
import im.vector.app.core.di.ActivityEntryPoint
import im.vector.app.core.dialogs.UnrecognizedCertificateDialog
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.giveAccessibilityFocus
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.utils.ToolbarConfig
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.navigation.Navigator
import im.vector.lib.strings.CommonStrings
import im.vector.lib.ui.styles.dialogs.MaterialProgressDialog
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import reactivecircus.flowbinding.android.view.clicks
import timber.log.Timber

abstract class VectorBaseFragment<VB : ViewBinding> : Fragment(), MavericksView {
    /* ==========================================================================================
     * Analytics
     * ========================================================================================== */

    protected var analyticsScreenName: MobileScreen.ScreenName? = null

    protected lateinit var analyticsTracker: AnalyticsTracker

    /* ==========================================================================================
     * Activity
     * ========================================================================================== */

    protected val vectorBaseActivity: VectorBaseActivity<*> by lazy {
        activity as VectorBaseActivity<*>
    }

    /* ==========================================================================================
     * Navigator and other common objects
     * ========================================================================================== */

    protected lateinit var navigator: Navigator
    protected lateinit var errorFormatter: ErrorFormatter
    protected lateinit var unrecognizedCertificateDialog: UnrecognizedCertificateDialog

    private var progress: AlertDialog? = null

    /**
     * [ToolbarConfig] instance from host activity.
     * */
    protected var toolbar: ToolbarConfig? = null
        get() = (activity as? VectorBaseActivity<*>)?.toolbar
        private set
    /* ==========================================================================================
     * View model
     * ========================================================================================== */

    private lateinit var viewModelFactory: ViewModelProvider.Factory

    protected val activityViewModelProvider
        get() = ViewModelProvider(requireActivity(), viewModelFactory)

    protected val fragmentViewModelProvider
        get() = ViewModelProvider(this, viewModelFactory)

    /* ==========================================================================================
     * Views
     * ========================================================================================== */

    private var _binding: VB? = null

    // This property is only valid between onCreateView and onDestroyView.
    protected val views: VB
        get() = _binding!!

    /* ==========================================================================================
     * Life cycle
     * ========================================================================================== */

    override fun onAttach(context: Context) {
        val singletonEntryPoint = context.singletonEntryPoint()
        val activityEntryPoint = EntryPointAccessors.fromActivity(vectorBaseActivity, ActivityEntryPoint::class.java)
        navigator = singletonEntryPoint.navigator()
        errorFormatter = singletonEntryPoint.errorFormatter()
        analyticsTracker = singletonEntryPoint.analyticsTracker()
        unrecognizedCertificateDialog = singletonEntryPoint.unrecognizedCertificateDialog()
        viewModelFactory = activityEntryPoint.viewModelFactory()
        super.onAttach(context)
    }

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("onCreate Fragment ${javaClass.simpleName}")
    }

    final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Timber.i("onCreateView Fragment ${javaClass.simpleName}")
        _binding = getBinding(inflater, container)
        return views.root
    }

    abstract fun getBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    @CallSuper
    override fun onResume() {
        super.onResume()
        Timber.i("onResume Fragment ${javaClass.simpleName}")
        analyticsScreenName?.let {
            analyticsTracker.screen(MobileScreen(screenName = it))
        }
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        Timber.i("onPause Fragment ${javaClass.simpleName}")
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("onViewCreated Fragment ${javaClass.simpleName}")
        setupMenu()
    }

    private fun setupMenu() {
        if (this !is VectorMenuProvider) return
        if (getMenuRes() == -1) return
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
                object : MenuProvider {
                    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                        menuInflater.inflate(getMenuRes(), menu)
                        handlePostCreateMenu(menu)
                    }

                    override fun onPrepareMenu(menu: Menu) {
                        handlePrepareMenu(menu)
                    }

                    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                        return handleMenuItemSelected(menuItem)
                    }
                },
                viewLifecycleOwner,
                Lifecycle.State.RESUMED
        )
    }

    open fun showLoading(message: CharSequence?) {
        showLoadingDialog(message)
    }

    open fun showFailure(throwable: Throwable) {
        displayErrorDialog(throwable)
    }

    @CallSuper
    override fun onDestroyView() {
        Timber.i("onDestroyView Fragment ${javaClass.simpleName}")
        _binding = null
        dismissLoadingDialog()
        super.onDestroyView()
    }

    @CallSuper
    override fun onDestroy() {
        Timber.i("onDestroy Fragment ${javaClass.simpleName}")
        super.onDestroy()
    }

    /* ==========================================================================================
     * Restorable
     * ========================================================================================== */

    private val restorables = ArrayList<Restorable>()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        restorables.forEach { it.onSaveInstanceState(outState) }
        restorables.clear()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        restorables.forEach { it.onRestoreInstanceState(savedInstanceState) }
        super.onViewStateRestored(savedInstanceState)
    }

    override fun invalidate() {
        // no-ops by default
        Timber.v("invalidate() method has not been implemented")
    }

    protected fun setArguments(args: Parcelable? = null) {
        arguments = args.toMvRxBundle()
    }

    @MainThread
    protected fun <T : Restorable> T.register(): T {
        assertMainThread()
        restorables.add(this)
        return this
    }

    protected fun showErrorInSnackbar(throwable: Throwable) {
        vectorBaseActivity.getCoordinatorLayout()?.showOptimizedSnackbar(errorFormatter.toHumanReadable(throwable))
    }

    protected fun showLoadingDialog(message: CharSequence? = null) {
        progress?.dismiss()
        progress = MaterialProgressDialog(requireContext())
                .show(message ?: getString(CommonStrings.please_wait))
    }

    protected fun dismissLoadingDialog() {
        progress?.dismiss()
    }

    /* ==========================================================================================
     * Toolbar
     * ========================================================================================== */

    /**
     * Sets toolbar as actionBar for current activity.
     *
     * @return Instance of [ToolbarConfig] with set of helper methods to configure toolbar
     * */
    protected fun setupToolbar(toolbar: MaterialToolbar): ToolbarConfig {
        return vectorBaseActivity.setupToolbar(toolbar)
    }

    /* ==========================================================================================
     * ViewEvents
     * ========================================================================================== */

    protected fun <T : VectorViewEvents> VectorViewModel<*, *, T>.observeViewEvents(
            observer: (T) -> Unit,
    ) {
        val tag = this@VectorBaseFragment::class.simpleName.toString()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewEvents
                        .stream(tag)
                        .collect {
                            dismissLoadingDialog()
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

    /* ==========================================================================================
     * MENU MANAGEMENT
     * ========================================================================================== */

    // This should be provided by the framework
    protected fun invalidateOptionsMenu() = requireActivity().invalidateOptionsMenu()

    /* ==========================================================================================
     * Common Dialogs
     * ========================================================================================== */

    protected fun displayErrorDialog(throwable: Throwable) {
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(CommonStrings.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(throwable))
                .setPositiveButton(CommonStrings.ok, null)
                .show()
    }

    /* ==========================================================================================
     * Accessibility - a11y
     * ========================================================================================== */

    private var hasBeenAccessibilityFocused = false

    /**
     * Ensure the View get the accessibility focus. This method has effect only once per fragment instance.
     */
    protected fun View.giveAccessibilityFocusOnce() {
        if (hasBeenAccessibilityFocused) return
        hasBeenAccessibilityFocused = true
        giveAccessibilityFocus()
    }
}
