/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.widgets

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.args
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.utils.CheckWebViewPermissionsUseCase
import im.vector.app.core.utils.openUrlInExternalBrowser
import im.vector.app.databinding.FragmentRoomWidgetBinding
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.webview.WebEventListener
import im.vector.app.features.widgets.webview.WebviewPermissionUtils
import im.vector.app.features.widgets.webview.clearAfterWidget
import im.vector.app.features.widgets.webview.setupForWidget
import im.vector.lib.core.utils.compat.resolveActivityCompat
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.terms.TermsService
import timber.log.Timber
import java.net.URISyntaxException
import javax.inject.Inject

@Parcelize
data class WidgetArgs(
        val baseUrl: String,
        val kind: WidgetKind,
        val roomId: String,
        val widgetId: String? = null,
        val urlParams: Map<String, String> = emptyMap()
) : Parcelable

@AndroidEntryPoint
class WidgetFragment :
        VectorBaseFragment<FragmentRoomWidgetBinding>(),
        WebEventListener,
        OnBackPressed,
        VectorMenuProvider {

    @Inject lateinit var permissionUtils: WebviewPermissionUtils
    @Inject lateinit var checkWebViewPermissionsUseCase: CheckWebViewPermissionsUseCase
    @Inject lateinit var vectorPreferences: VectorPreferences

    private val fragmentArgs: WidgetArgs by args()
    private val viewModel: WidgetViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomWidgetBinding {
        return FragmentRoomWidgetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.widgetWebView.setupForWidget(requireActivity(), checkWebViewPermissionsUseCase, this)
        if (fragmentArgs.kind.isAdmin()) {
            viewModel.getPostAPIMediator().setWebView(views.widgetWebView)
        }
        viewModel.observeViewEvents {
            Timber.v("Observed view events: $it")
            when (it) {
                is WidgetViewEvents.DisplayTerms -> displayTerms(it)
                is WidgetViewEvents.OnURLFormatted -> loadFormattedUrl(it)
                is WidgetViewEvents.DisplayIntegrationManager -> displayIntegrationManager(it)
                is WidgetViewEvents.Failure -> displayErrorDialog(it.throwable)
                is WidgetViewEvents.Close -> Unit
            }
        }
        viewModel.handle(WidgetAction.LoadFormattedUrl)
    }

    private val termsActivityResultLauncher = registerStartForActivityResult {
        Timber.v("On terms results")
        if (it.resultCode == Activity.RESULT_OK) {
            viewModel.handle(WidgetAction.OnTermsReviewed)
        } else {
            vectorBaseActivity.finish()
        }
    }

    private val integrationManagerActivityResultLauncher = registerStartForActivityResult {
        if (it.resultCode == Activity.RESULT_OK) {
            viewModel.handle(WidgetAction.LoadFormattedUrl)
        }
    }

    override fun onDestroyView() {
        if (fragmentArgs.kind.isAdmin()) {
            viewModel.getPostAPIMediator().clearWebView()
        }
        views.widgetWebView.clearAfterWidget()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        views.widgetWebView.let {
            it.resumeTimers()
            it.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (fragmentArgs.kind != WidgetKind.ELEMENT_CALL) {
            views.widgetWebView.let {
                it.pauseTimers()
                it.onPause()
            }
        }
    }

    override fun getMenuRes() = R.menu.menu_widget

    override fun handlePrepareMenu(menu: Menu) {
        withState(viewModel) { state ->
            val widget = state.asyncWidget()
            menu.findItem(R.id.action_edit)?.isVisible = state.widgetKind != WidgetKind.INTEGRATION_MANAGER
            if (widget == null) {
                menu.findItem(R.id.action_refresh)?.isVisible = false
                menu.findItem(R.id.action_widget_open_ext)?.isVisible = false
                menu.findItem(R.id.action_delete)?.isVisible = false
                menu.findItem(R.id.action_revoke)?.isVisible = false
            } else {
                menu.findItem(R.id.action_refresh)?.isVisible = true
                menu.findItem(R.id.action_widget_open_ext)?.isVisible = true
                menu.findItem(R.id.action_delete)?.isVisible = state.canManageWidgets && widget.isAddedByMe
                menu.findItem(R.id.action_revoke)?.isVisible = state.status == WidgetStatus.WIDGET_ALLOWED && !widget.isAddedByMe
            }
        }
    }

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return withState(viewModel) { state ->
            return@withState when (item.itemId) {
                R.id.action_edit -> {
                    navigator.openIntegrationManager(
                            requireContext(),
                            integrationManagerActivityResultLauncher,
                            state.roomId,
                            state.widgetId,
                            state.widgetKind.screenId
                    )
                    true
                }
                R.id.action_delete -> {
                    deleteWidget()
                    true
                }
                R.id.action_refresh -> {
                    if (state.formattedURL.complete) {
                        views.widgetWebView.reload()
                    }
                    true
                }
                R.id.action_widget_open_ext -> {
                    if (state.formattedURL.complete) {
                        openUrlInExternalBrowser(requireContext(), state.formattedURL.invoke())
                    }
                    true
                }
                R.id.action_revoke -> {
                    if (state.status == WidgetStatus.WIDGET_ALLOWED) {
                        revokeWidget()
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean = withState(viewModel) { state ->
        if (state.formattedURL.complete) {
            if (views.widgetWebView.canGoBack()) {
                views.widgetWebView.goBack()
                return@withState true
            }
        }
        return@withState false
    }

    override fun invalidate() = withState(viewModel) { state ->
        Timber.v("Invalidate state: $state")
        when (val formattedUrl = state.formattedURL) {
            Uninitialized,
            is Loading -> {
                setStateError(null)
                views.widgetWebView.isInvisible = true
                views.widgetProgressBar.isIndeterminate = true
                views.widgetProgressBar.isVisible = true
            }
            is Success -> {
                if (views.widgetWebView.url == null) {
                    loadFormattedUrl(formattedUrl())
                }
                setStateError(null)
                when (state.webviewLoadedUrl) {
                    Uninitialized -> {
                        views.widgetWebView.isInvisible = true
                    }
                    is Loading -> {
                        setStateError(null)
                        views.widgetWebView.isInvisible = false
                        views.widgetProgressBar.isIndeterminate = true
                        views.widgetProgressBar.isVisible = true
                    }
                    is Success -> {
                        views.widgetWebView.isInvisible = false
                        views.widgetProgressBar.isVisible = false
                        setStateError(null)
                    }
                    is Fail -> {
                        views.widgetProgressBar.isInvisible = true
                        setStateError(state.webviewLoadedUrl.error.message)
                    }
                }
            }
            is Fail -> {
                // we need to show Error
                views.widgetWebView.isInvisible = true
                views.widgetProgressBar.isVisible = false
                setStateError(formattedUrl.error.message)
            }
        }
    }

    override fun shouldOverrideUrlLoading(url: String): Boolean {
        if (url.startsWith("intent://")) {
            try {
                val context = requireContext()
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                if (intent != null) {
                    val packageManager: PackageManager = context.packageManager
                    val info = packageManager.resolveActivityCompat(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    if (info != null) {
                        context.startActivity(intent)
                    } else {
                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                        openUrlInExternalBrowser(context, fallbackUrl)
                    }
                    return true
                }
            } catch (e: URISyntaxException) {
                Timber.d("Can't resolve intent://")
            }
        }
        return false
    }

    override fun onPageStarted(url: String) {
        viewModel.handle(WidgetAction.OnWebViewStartedToLoad(url))
    }

    override fun onPageFinished(url: String) {
        viewModel.handle(WidgetAction.OnWebViewLoadingSuccess(url))
    }

    override fun onPageError(url: String, errorCode: Int, description: String) {
        viewModel.handle(WidgetAction.OnWebViewLoadingError(url, false, errorCode, description))
    }

    override fun onHttpError(url: String, errorCode: Int, description: String) {
        viewModel.handle(WidgetAction.OnWebViewLoadingError(url, true, errorCode, description))
    }

    private val permissionResultLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionUtils.onPermissionResult(result)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        permissionUtils.promptForPermissions(
                title = CommonStrings.room_widget_resource_permission_title,
                request = request,
                context = requireContext(),
                activity = requireActivity(),
                activityResultLauncher = permissionResultLauncher,
                autoApprove = fragmentArgs.kind == WidgetKind.ELEMENT_CALL && vectorPreferences.labsEnableElementCallPermissionShortcuts()
        )
    }

    private fun displayTerms(displayTerms: WidgetViewEvents.DisplayTerms) {
        navigator.openTerms(
                context = requireContext(),
                activityResultLauncher = termsActivityResultLauncher,
                serviceType = TermsService.ServiceType.IntegrationManager,
                baseUrl = displayTerms.url,
                token = displayTerms.token
        )
    }

    private fun loadFormattedUrl(event: WidgetViewEvents.OnURLFormatted) {
        loadFormattedUrl(event.formattedURL)
    }

    private fun loadFormattedUrl(formattedUrl: String) {
        views.widgetWebView.clearHistory()
        views.widgetWebView.loadUrl(formattedUrl)
    }

    private fun setStateError(message: String?) {
        if (message == null) {
            views.widgetErrorLayout.isVisible = false
            views.widgetErrorText.text = null
        } else {
            views.widgetProgressBar.isVisible = false
            views.widgetErrorLayout.isVisible = true
            views.widgetWebView.isInvisible = true
            views.widgetErrorText.text = getString(CommonStrings.room_widget_failed_to_load, message)
        }
    }

    private fun displayIntegrationManager(event: WidgetViewEvents.DisplayIntegrationManager) {
        navigator.openIntegrationManager(
                context = requireContext(),
                activityResultLauncher = integrationManagerActivityResultLauncher,
                roomId = fragmentArgs.roomId,
                integId = event.integId,
                screen = event.integType
        )
    }

    private fun deleteWidget() {
        MaterialAlertDialogBuilder(requireContext())
                .setMessage(CommonStrings.widget_delete_message_confirmation)
                .setPositiveButton(CommonStrings.action_remove) { _, _ ->
                    viewModel.handle(WidgetAction.DeleteWidget)
                }
                .setNegativeButton(CommonStrings.action_cancel, null)
                .show()
    }

    private fun revokeWidget() {
        viewModel.handle(WidgetAction.RevokeWidget)
    }
}
