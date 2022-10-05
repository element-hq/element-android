/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.settings.devices.v2.othersessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
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
import im.vector.app.features.settings.devices.v2.more.SessionLearnMoreBottomSheet
import im.vector.app.features.themes.ThemeUtils
import javax.inject.Inject

@AndroidEntryPoint
class OtherSessionsFragment :
        VectorBaseFragment<FragmentOtherSessionsBinding>(),
        VectorBaseBottomSheetDialogFragment.ResultListener,
        OtherSessionsView.Callback {

    private val viewModel: OtherSessionsViewModel by fragmentViewModel()
    private val args: OtherSessionsArgs by args()

    @Inject lateinit var colorProvider: ColorProvider

    @Inject lateinit var viewNavigator: OtherSessionsViewNavigator

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOtherSessionsBinding {
        return FragmentOtherSessionsBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.otherSessionsToolbar).setTitle(args.titleResourceId).allowBack()
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
                updateSecurityLearnMoreButton(R.string.device_manager_learn_more_sessions_verified_title, R.string.device_manager_learn_more_sessions_verified)
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
                updateSecurityLearnMoreButton(
                        R.string.device_manager_learn_more_sessions_unverified_title,
                        R.string.device_manager_learn_more_sessions_unverified
                )
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
                updateSecurityLearnMoreButton(R.string.device_manager_learn_more_sessions_inactive_title, R.string.device_manager_learn_more_sessions_inactive)
            }
            DeviceManagerFilterType.ALL_SESSIONS -> { /* NOOP. View is not visible */
            }
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
