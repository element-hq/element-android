/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.othersessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment.ResultListener.Companion.RESULT_OK
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.databinding.FragmentOtherSessionsBinding
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterBottomSheet
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.features.settings.devices.v2.list.OtherSessionsView
import im.vector.app.features.settings.devices.v2.list.SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS
import im.vector.app.features.themes.ThemeUtils
import javax.inject.Inject

@AndroidEntryPoint
class OtherSessionsFragment :
        VectorBaseFragment<FragmentOtherSessionsBinding>(),
        VectorBaseBottomSheetDialogFragment.ResultListener,
        OtherSessionsView.Callback {

    private val viewModel: OtherSessionsViewModel by fragmentViewModel()

    @Inject lateinit var colorProvider: ColorProvider

    @Inject lateinit var viewNavigator: OtherSessionsViewNavigator

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOtherSessionsBinding {
        return FragmentOtherSessionsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.otherSessionsToolbar).allowBack()
        observeViewEvents()
        initFilterView()
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                is OtherSessionsViewEvents.Loading -> showLoading(it.message)
                is OtherSessionsViewEvents.Failure -> showFailure(it.throwable)
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
    }

    override fun onBottomSheetResult(resultCode: Int, data: Any?) {
        if (resultCode == RESULT_OK && data != null && data is DeviceManagerFilterType) {
            viewModel.handle(OtherSessionsAction.FilterDevices(data))
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        if (state.devices is Success) {
            renderDevices(state.devices(), state.currentFilter)
        }
    }

    private fun renderDevices(devices: List<DeviceFullInfo>?, currentFilter: DeviceManagerFilterType) {
        views.otherSessionsFilterBadgeImageView.isVisible = currentFilter != DeviceManagerFilterType.ALL_SESSIONS
        views.otherSessionsSecurityRecommendationView.isVisible = currentFilter != DeviceManagerFilterType.ALL_SESSIONS
        views.deviceListHeaderOtherSessions.isVisible = currentFilter == DeviceManagerFilterType.ALL_SESSIONS

        when (currentFilter) {
            DeviceManagerFilterType.VERIFIED -> {
                views.otherSessionsSecurityRecommendationView.render(
                        OtherSessionsSecurityRecommendationViewState(
                                title = getString(R.string.device_manager_other_sessions_recommendation_title_verified),
                                description = getString(R.string.device_manager_other_sessions_recommendation_description_verified),
                                imageResourceId = R.drawable.ic_shield_trusted_no_border,
                                imageTintColorResourceId = colorProvider.getColor(R.color.shield_color_trust_background)
                        )
                )
                views.otherSessionsNotFoundTextView.text = getString(R.string.device_manager_other_sessions_no_verified_sessions_found)
            }
            DeviceManagerFilterType.UNVERIFIED -> {
                views.otherSessionsSecurityRecommendationView.render(
                        OtherSessionsSecurityRecommendationViewState(
                                title = getString(R.string.device_manager_other_sessions_recommendation_title_unverified),
                                description = getString(R.string.device_manager_other_sessions_recommendation_description_unverified),
                                imageResourceId = R.drawable.ic_shield_warning_no_border,
                                imageTintColorResourceId = colorProvider.getColor(R.color.shield_color_warning_background)
                        )
                )
                views.otherSessionsNotFoundTextView.text = getString(R.string.device_manager_other_sessions_no_unverified_sessions_found)
            }
            DeviceManagerFilterType.INACTIVE -> {
                views.otherSessionsSecurityRecommendationView.render(
                        OtherSessionsSecurityRecommendationViewState(
                                title = getString(R.string.device_manager_other_sessions_recommendation_title_inactive),
                                description = resources.getQuantityString(
                                        R.plurals.device_manager_other_sessions_recommendation_description_inactive,
                                        SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                                        SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS
                                ),
                                imageResourceId = R.drawable.ic_inactive_sessions,
                                imageTintColorResourceId = ThemeUtils.getColor(requireContext(), R.attr.vctr_system)
                        )
                )
                views.otherSessionsNotFoundTextView.text = getString(R.string.device_manager_other_sessions_no_inactive_sessions_found)
            }
            DeviceManagerFilterType.ALL_SESSIONS -> { /* NOOP. View is not visible */ }
        }

        if (devices.isNullOrEmpty()) {
            views.deviceListOtherSessions.isVisible = false
            views.otherSessionsNotFoundLayout.isVisible = true
        } else {
            views.deviceListOtherSessions.isVisible = true
            views.otherSessionsNotFoundLayout.isVisible = false
            views.deviceListOtherSessions.render(devices = devices, totalNumberOfDevices = devices.size, showViewAll = false)
        }
    }

    override fun onOtherSessionClicked(deviceId: String) {
        viewNavigator.navigateToSessionOverview(
                context = requireActivity(),
                deviceId = deviceId
        )
    }

    override fun onViewAllOtherSessionsClicked() {
        // NOOP. We don't have this button in this screen
    }
}
