/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.othersessions

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.setTextColor
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment.ResultListener.Companion.RESULT_OK
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.databinding.FragmentOtherSessionsBinding
import im.vector.app.features.auth.ReAuthActivity
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterBottomSheet
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.features.settings.devices.v2.list.OtherSessionsView
import im.vector.app.features.settings.devices.v2.list.SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS
import im.vector.app.features.settings.devices.v2.more.SessionLearnMoreBottomSheet
import im.vector.app.features.settings.devices.v2.signout.BuildConfirmSignoutDialogUseCase
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

@AndroidEntryPoint
class OtherSessionsFragment :
        VectorBaseFragment<FragmentOtherSessionsBinding>(),
        VectorBaseBottomSheetDialogFragment.ResultListener,
        OtherSessionsView.Callback,
        VectorMenuProvider {

    private val viewModel: OtherSessionsViewModel by fragmentViewModel()
    private val args: OtherSessionsArgs by args()

    @Inject lateinit var colorProvider: ColorProvider

    @Inject lateinit var stringProvider: StringProvider

    @Inject lateinit var viewNavigator: OtherSessionsViewNavigator

    @Inject lateinit var buildConfirmSignoutDialogUseCase: BuildConfirmSignoutDialogUseCase

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOtherSessionsBinding {
        return FragmentOtherSessionsBinding.inflate(layoutInflater, container, false)
    }

    override fun getMenuRes() = R.menu.menu_other_sessions

    override fun handlePrepareMenu(menu: Menu) {
        withState(viewModel) { state ->
            val isSelectModeEnabled = state.isSelectModeEnabled
            menu.findItem(R.id.otherSessionsSelectAll).isVisible = isSelectModeEnabled
            menu.findItem(R.id.otherSessionsDeselectAll).isVisible = isSelectModeEnabled
            menu.findItem(R.id.otherSessionsSelect).isVisible = !isSelectModeEnabled && state.devices()?.isNotEmpty().orFalse()
            menu.findItem(R.id.otherSessionsToggleIpAddress).isVisible = !isSelectModeEnabled
            menu.findItem(R.id.otherSessionsToggleIpAddress).title = if (state.isShowingIpAddress) {
                getString(CommonStrings.device_manager_other_sessions_hide_ip_address)
            } else {
                getString(CommonStrings.device_manager_other_sessions_show_ip_address)
            }
            updateMultiSignoutMenuItem(menu, state)
        }
    }

    private fun updateMultiSignoutMenuItem(menu: Menu, viewState: OtherSessionsViewState) {
        val multiSignoutItem = menu.findItem(R.id.otherSessionsMultiSignout)
        multiSignoutItem.title = if (viewState.isSelectModeEnabled) {
            getString(CommonStrings.device_manager_other_sessions_multi_signout_selection).uppercase()
        } else {
            val nbDevices = viewState.devices()?.size ?: 0
            stringProvider.getQuantityString(CommonPlurals.device_manager_other_sessions_multi_signout_all, nbDevices, nbDevices)
        }
        multiSignoutItem.isVisible = if (viewState.delegatedOidcAuthEnabled) {
            // Hide multi signout if the homeserver delegates the account management
            false
        } else {
            if (viewState.isSelectModeEnabled) {
                viewState.devices.invoke()?.any { it.isSelected }.orFalse()
            } else {
                viewState.devices.invoke()?.isNotEmpty().orFalse()
            }
        }
        val showAsActionFlag = if (viewState.isSelectModeEnabled) MenuItem.SHOW_AS_ACTION_IF_ROOM else MenuItem.SHOW_AS_ACTION_NEVER
        multiSignoutItem.setShowAsAction(showAsActionFlag or MenuItem.SHOW_AS_ACTION_WITH_TEXT)
        changeTextColorOfDestructiveAction(multiSignoutItem)
    }

    private fun changeTextColorOfDestructiveAction(menuItem: MenuItem) {
        val titleColor = colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorError)
        menuItem.setTextColor(titleColor)
    }

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.otherSessionsSelect -> {
                enableSelectMode(true)
                true
            }
            R.id.otherSessionsSelectAll -> {
                viewModel.handle(OtherSessionsAction.SelectAll)
                true
            }
            R.id.otherSessionsDeselectAll -> {
                viewModel.handle(OtherSessionsAction.DeselectAll)
                true
            }
            R.id.otherSessionsMultiSignout -> {
                confirmMultiSignout()
                true
            }
            R.id.otherSessionsToggleIpAddress -> {
                toggleIpAddressVisibility()
                true
            }
            else -> false
        }
    }

    private fun toggleIpAddressVisibility() {
        viewModel.handle(OtherSessionsAction.ToggleIpAddressVisibility)
    }

    private fun confirmMultiSignout() {
        activity?.let {
            buildConfirmSignoutDialogUseCase.execute(it, this::multiSignout)
                    .show()
        }
    }

    private fun multiSignout() {
        viewModel.handle(OtherSessionsAction.MultiSignout)
    }

    private fun enableSelectMode(isEnabled: Boolean, deviceId: String? = null) {
        val action = if (isEnabled) OtherSessionsAction.EnableSelectMode(deviceId) else OtherSessionsAction.DisableSelectMode
        viewModel.handle(action)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(owner = this) {
            handleBackPress(this)
        }
    }

    private fun handleBackPress(onBackPressedCallback: OnBackPressedCallback) = withState(viewModel) { state ->
        if (state.isSelectModeEnabled) {
            enableSelectMode(false)
        } else {
            onBackPressedCallback.isEnabled = false
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.otherSessionsToolbar)
                .setTitle(CommonStrings.device_manager_sessions_other_title)
                .allowBack()
        observeViewEvents()
        initFilterView()
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                is OtherSessionsViewEvents.SignoutError -> showFailure(it.error)
                is OtherSessionsViewEvents.RequestReAuth -> askForReAuthentication(it)
                OtherSessionsViewEvents.SignoutSuccess -> enableSelectMode(false)
            }
        }
    }

    private fun initFilterView() {
        views.otherSessionsFilterFrameLayout.debouncedClicks {
            withState(viewModel) { state ->
                DeviceManagerFilterBottomSheet
                        .newInstance(state.currentFilter, this)
                        .show(requireActivity().supportFragmentManager, "SHOW_DEVICE_MANAGER_FILTER_BOTTOM_SHEET")
            }
        }

        views.otherSessionsClearFilterButton.debouncedClicks {
            viewModel.handle(OtherSessionsAction.FilterDevices(DeviceManagerFilterType.ALL_SESSIONS))
        }

        views.deviceListOtherSessions.callback = this

        if (args.defaultFilter != DeviceManagerFilterType.ALL_SESSIONS) {
            viewModel.handle(OtherSessionsAction.FilterDevices(args.defaultFilter))
        }
    }

    override fun onBottomSheetResult(resultCode: Int, data: Any?) {
        if (resultCode == RESULT_OK && data != null && data is DeviceManagerFilterType) {
            viewModel.handle(OtherSessionsAction.FilterDevices(data))
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        updateLoading(state.isLoading)
        updateFilterView(state.isSelectModeEnabled)
        if (state.devices is Success) {
            val devices = state.devices.invoke()
            renderDevices(devices, state.currentFilter, state.isShowingIpAddress)
            updateToolbar(devices, state.isSelectModeEnabled)
        }
    }

    private fun updateLoading(isLoading: Boolean) {
        if (isLoading) {
            showLoading(null)
        } else {
            dismissLoadingDialog()
        }
    }

    private fun updateFilterView(isSelectModeEnabled: Boolean) {
        views.otherSessionsFilterFrameLayout.isVisible = isSelectModeEnabled.not()
    }

    private fun updateToolbar(devices: List<DeviceFullInfo>, isSelectModeEnabled: Boolean) {
        invalidateOptionsMenu()
        val title = if (isSelectModeEnabled) {
            val selection = devices.count { it.isSelected }
            stringProvider.getQuantityString(CommonPlurals.x_selected, selection, selection)
        } else {
            getString(CommonStrings.device_manager_sessions_other_title)
        }
        toolbar?.title = title
    }

    private fun renderDevices(devices: List<DeviceFullInfo>, currentFilter: DeviceManagerFilterType, isShowingIpAddress: Boolean) {
        views.otherSessionsFilterBadgeImageView.isVisible = currentFilter != DeviceManagerFilterType.ALL_SESSIONS
        views.otherSessionsSecurityRecommendationView.isVisible = currentFilter != DeviceManagerFilterType.ALL_SESSIONS
        views.deviceListHeaderOtherSessions.isVisible = currentFilter == DeviceManagerFilterType.ALL_SESSIONS

        when (currentFilter) {
            DeviceManagerFilterType.VERIFIED -> {
                views.otherSessionsSecurityRecommendationView.render(
                        OtherSessionsSecurityRecommendationViewState(
                                title = getString(CommonStrings.device_manager_other_sessions_recommendation_title_verified),
                                description = getString(CommonStrings.device_manager_other_sessions_recommendation_description_verified),
                                imageResourceId = R.drawable.ic_shield_trusted_no_border,
                                imageTintColorResourceId = colorProvider.getColor(im.vector.lib.ui.styles.R.color.shield_color_trust_background)
                        )
                )
                views.otherSessionsNotFoundTextView.text = getString(CommonStrings.device_manager_other_sessions_no_verified_sessions_found)
                updateSecurityLearnMoreButton(
                        CommonStrings.device_manager_learn_more_sessions_verified_title,
                        CommonStrings.device_manager_learn_more_sessions_verified_description
                )
            }
            DeviceManagerFilterType.UNVERIFIED -> {
                views.otherSessionsSecurityRecommendationView.render(
                        OtherSessionsSecurityRecommendationViewState(
                                title = getString(CommonStrings.device_manager_other_sessions_recommendation_title_unverified),
                                description = getString(CommonStrings.device_manager_other_sessions_recommendation_description_unverified),
                                imageResourceId = R.drawable.ic_shield_warning_no_border,
                                imageTintColorResourceId = colorProvider.getColor(im.vector.lib.ui.styles.R.color.shield_color_warning_background)
                        )
                )
                views.otherSessionsNotFoundTextView.text = getString(CommonStrings.device_manager_other_sessions_no_unverified_sessions_found)
                updateSecurityLearnMoreButton(
                        CommonStrings.device_manager_learn_more_sessions_unverified_title,
                        CommonStrings.device_manager_learn_more_sessions_unverified
                )
            }
            DeviceManagerFilterType.INACTIVE -> {
                views.otherSessionsSecurityRecommendationView.render(
                        OtherSessionsSecurityRecommendationViewState(
                                title = getString(CommonStrings.device_manager_other_sessions_recommendation_title_inactive),
                                description = resources.getQuantityString(
                                        CommonPlurals.device_manager_other_sessions_recommendation_description_inactive,
                                        SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                                        SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS
                                ),
                                imageResourceId = R.drawable.ic_inactive_sessions,
                                imageTintColorResourceId = ThemeUtils.getColor(requireContext(), im.vector.lib.ui.styles.R.attr.vctr_system)
                        )
                )
                views.otherSessionsNotFoundTextView.text = getString(CommonStrings.device_manager_other_sessions_no_inactive_sessions_found)
                updateSecurityLearnMoreButton(
                        CommonStrings.device_manager_learn_more_sessions_inactive_title,
                        CommonStrings.device_manager_learn_more_sessions_inactive
                )
            }
            DeviceManagerFilterType.ALL_SESSIONS -> { /* NOOP. View is not visible */
            }
        }

        if (devices.isEmpty()) {
            views.deviceListOtherSessions.isVisible = false
            views.otherSessionsNotFoundLayout.isVisible = true
        } else {
            views.deviceListOtherSessions.isVisible = true
            views.otherSessionsNotFoundLayout.isVisible = false
            val mappedDevices = if (isShowingIpAddress) devices else devices.map { it.copy(deviceInfo = it.deviceInfo.copy(lastSeenIp = null)) }
            views.deviceListOtherSessions.render(devices = mappedDevices, totalNumberOfDevices = mappedDevices.size, showViewAll = false)
        }
    }

    private fun updateSecurityLearnMoreButton(
            @StringRes titleResId: Int,
            @StringRes descriptionResId: Int,
    ) {
        views.otherSessionsSecurityRecommendationView.onLearnMoreClickListener = {
            showLearnMoreInfo(titleResId, getString(descriptionResId))
        }
    }

    private fun showLearnMoreInfo(
            @StringRes titleResId: Int,
            description: String,
    ) {
        val args = SessionLearnMoreBottomSheet.Args(
                title = getString(titleResId),
                description = description,
        )
        SessionLearnMoreBottomSheet.show(childFragmentManager, args)
    }

    override fun onOtherSessionLongClicked(deviceId: String) = withState(viewModel) { state ->
        if (!state.isSelectModeEnabled) {
            enableSelectMode(true, deviceId)
        } else {
            viewModel.handle(OtherSessionsAction.ToggleSelectionForDevice(deviceId))
        }
    }

    override fun onOtherSessionClicked(deviceId: String) = withState(viewModel) { state ->
        if (state.isSelectModeEnabled) {
            viewModel.handle(OtherSessionsAction.ToggleSelectionForDevice(deviceId))
        } else {
            viewNavigator.navigateToSessionOverview(
                    context = requireActivity(),
                    deviceId = deviceId
            )
        }
    }

    override fun onViewAllOtherSessionsClicked() {
        // NOOP. We don't have this button in this screen
    }

    private val reAuthActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            when (activityResult.data?.extras?.getString(ReAuthActivity.RESULT_FLOW_TYPE)) {
                LoginFlowTypes.SSO -> {
                    viewModel.handle(OtherSessionsAction.SsoAuthDone)
                }
                LoginFlowTypes.PASSWORD -> {
                    val password = activityResult.data?.extras?.getString(ReAuthActivity.RESULT_VALUE) ?: ""
                    viewModel.handle(OtherSessionsAction.PasswordAuthDone(password))
                }
                else -> {
                    viewModel.handle(OtherSessionsAction.ReAuthCancelled)
                }
            }
        } else {
            viewModel.handle(OtherSessionsAction.ReAuthCancelled)
        }
    }

    /**
     * Launch the re auth activity to get credentials.
     */
    private fun askForReAuthentication(reAuthReq: OtherSessionsViewEvents.RequestReAuth) {
        ReAuthActivity.newIntent(
                requireContext(),
                reAuthReq.registrationFlowResponse,
                reAuthReq.lastErrorCode,
                getString(CommonStrings.devices_delete_dialog_title)
        ).let { intent ->
            reAuthActivityResultLauncher.launch(intent)
        }
    }
}
